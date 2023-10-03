package io.boomerang.service;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import io.boomerang.error.BoomerangException;
import io.boomerang.kube.exception.KubeRuntimeException;
import io.boomerang.kube.service.KubeServiceImpl;
import io.boomerang.kube.service.TektonServiceImpl;
import io.boomerang.model.TaskResponse;
import io.boomerang.model.ref.RunResult;
import io.boomerang.model.ref.TaskDeletionEnum;
import io.boomerang.model.ref.TaskRun;
import io.boomerang.model.ref.TaskType;
import io.fabric8.kubernetes.client.KubernetesClientException;

@Service
public class TaskServiceImpl implements TaskService {

  private static final Logger LOGGER = LogManager.getLogger(TaskServiceImpl.class);

  @Value("${kube.timeout.waitUntil}")
  protected long waitUntilTimeout;

  @Value("${kube.task.deletion}")
  private TaskDeletionEnum taskDeletion;

  @Value("${kube.task.timeout}")
  private Long taskTimeout;

  @Autowired
  private KubeServiceImpl kubeService;

  @Autowired
  private TektonServiceImpl tektonService;

  @Autowired
  private DeleteService deleteService;

  protected TaskDeletionEnum getTaskDeletion(TaskDeletionEnum deletion) {
    return deletion != null ? deletion : taskDeletion;
  }

  protected Long getTaskTimeout(Long timeout) {
    return timeout != null && timeout != 0 ? timeout : taskTimeout;
  }

  @Override
  public TaskResponse terminate(TaskRun task) {
    TaskResponse response =
        new TaskResponse("0", "Task (" + task.getId() + ") is meant to be terminated now.", null);

    tektonService.cancelTaskRun(task.getWorkflowRef(), task.getWorkflowRunRef(), task.getId(),
        task.getLabels());

    return response;
  }

  @Override
  public TaskResponse execute(TaskRun task) {
    TaskResponse response =
        new TaskResponse("0", "Task (" + task.getId() + ") has been executed successfully.", null);
    List<RunResult> results = new ArrayList<>();
    if (!TaskType.script.equals(task.getType()) && task.getSpec().getImage() == null) {
      throw new BoomerangException(1, "NO_TASK_IMAGE", HttpStatus.BAD_REQUEST,
          task.getClass().toString());
    } else {
      try {
        kubeService.createTaskConfigMap(task.getWorkflowRef(), task.getWorkflowRunRef(),
            task.getName(), task.getId(), task.getLabels(), task.getParams());
        tektonService.createTaskRun(task.getWorkflowRef(), task.getWorkflowRunRef(), task.getId(),
            task.getName(), task.getLabels(), task.getSpec().getImage(),
            task.getSpec().getCommand(), task.getSpec().getScript(), task.getSpec().getArguments(), task.getParams(),
            task.getSpec().getEnvs(), task.getResults(), task.getSpec().getWorkingDir(),
            task.getWorkspaces(), waitUntilTimeout, getTaskTimeout(task.getTimeout()),
            task.getSpec().getDebug());
        results = tektonService.watchTaskRun(task.getWorkflowRef(), task.getWorkflowRunRef(),
            task.getId(), task.getLabels(), getTaskTimeout(task.getTimeout()));
        if (getTaskDeletion(task.getSpec().getDeletion())
            .equals(TaskDeletionEnum.OnSuccess)) {
          // This will only delete on success as failure throws an Exception.
          deleteService.deleteTaskRun(task.getWorkflowRef(), task.getWorkflowRunRef(), task.getId(),
              task.getLabels());
        }
      } catch (KubernetesClientException e) {
        // KubernetesClientException handles the case where an internal admission
        // controller rejects the creation
        if (e.getMessage().contains("admission webhook")) {
          LOGGER.info(e.toString());
          throw new BoomerangException(1, "ADMISSION_WEBHOOK_DENIED", HttpStatus.BAD_REQUEST,
              e.getMessage());
        } else {
          throw new BoomerangException(e, 1, e.toString(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
      } catch (KubeRuntimeException e) {
        LOGGER.info("DEBUG::Task Is Being Set as Failed");
        throw new BoomerangException(e, 1, e.toString(), HttpStatus.INTERNAL_SERVER_ERROR);
      } catch (InterruptedException e) {
        throw new BoomerangException(1, "TASK_CREATION_ERROR", HttpStatus.INTERNAL_SERVER_ERROR,
            e.getMessage());
      } catch (ParseException e) {
        throw new BoomerangException(1, "TASK_CREATION_TIMEOUT_ERROR",
            HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
      } finally {
        response.setResults(results);
        kubeService.deleteTaskConfigMap(task.getWorkflowRef(), task.getWorkflowRunRef(),
            task.getId(), task.getLabels());
        if (getTaskDeletion(task.getSpec().getDeletion())
            .equals(TaskDeletionEnum.Always)) {
          deleteService.deleteTaskRun(task.getWorkflowRef(), task.getWorkflowRunRef(), task.getId(),
              task.getLabels());
        }
        LOGGER.info("Task (" + task.getId() + ") has completed with code " + response.getCode());
      }
    }
    return response;
  }
}
