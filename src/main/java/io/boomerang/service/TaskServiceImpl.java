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
import io.boomerang.model.Task;
import io.boomerang.model.TaskConfiguration;
import io.boomerang.model.TaskCustom;
import io.boomerang.model.TaskDeletionEnum;
import io.boomerang.model.TaskResponse;
import io.boomerang.model.TaskResponseResultParameter;
import io.boomerang.model.TaskTemplate;
import io.fabric8.kubernetes.client.KubernetesClientException;

@Service
public class TaskServiceImpl implements TaskService {

  private static final Logger LOGGER = LogManager.getLogger(TaskServiceImpl.class);
  
  @Value("${kube.timeout.waitUntil}")
  protected long waitUntilTimeout;

  @Value("${kube.task.deletion}")
  private TaskDeletionEnum taskDeletion;

  @Value("${kube.task.timeout}")
  private Integer taskTimeout;

  @Autowired
  private KubeServiceImpl kubeService;

  @Autowired
  private TektonServiceImpl tektonService;

  @Autowired
  private DeleteService deleteService;

  protected TaskDeletionEnum getTaskDeletionConfig(TaskConfiguration taskConfiguration) {
    return taskConfiguration != null && taskConfiguration.getDeletion() != null
        ? taskConfiguration.getDeletion()
        : taskDeletion;
  }

  protected Integer getTaskTimeout(TaskConfiguration taskConfiguration) {
    return taskConfiguration != null && taskConfiguration.getTimeout() != 0
        ? taskConfiguration.getTimeout()
        : taskTimeout;
  }

  @Override
  public TaskResponse terminateTask(Task task) {
    TaskResponse response = new TaskResponse("0",
        "Task (" + task.getTaskId() + ") is meant to be terminated now.", null);
    
    tektonService.cancelTask(task.getWorkflowId(), task.getWorkflowActivityId(), task.getTaskId(), task.getTaskActivityId(),
              task.getLabels());

    return response;
  }

  @Override
  public TaskResponse executeTask(Task task) {
    if (task instanceof TaskTemplate) {
      return executeTaskTemplate((TaskTemplate) task);
    } else if (task instanceof TaskCustom) {
       return executeTaskCustom((TaskCustom)task);
    } else {
      throw new BoomerangException(1, "UNKOWN_TASK_TYPE", HttpStatus.BAD_REQUEST,
          task.getClass().toString());
    }
  }

  private TaskResponse executeTaskTemplate(TaskTemplate task) {
    TaskResponse response = new TaskResponse("0",
        "Task (" + task.getTaskId() + ") has been executed successfully.", null);
    List<TaskResponseResultParameter> results = new ArrayList<>();
    if (task.getImage() == null) {
      throw new BoomerangException(1, "NO_TASK_IMAGE", HttpStatus.BAD_REQUEST,
          task.getClass().toString());
    } else {
      try {
        kubeService.createTaskConfigMap(task.getWorkflowName(), task.getWorkflowId(),
            task.getWorkflowActivityId(), task.getTaskName(), task.getTaskId(),
            task.getTaskActivityId(), task.getLabels(), task.getParameters());
        tektonService.createTaskRun(task.getWorkflowName(), task.getWorkflowId(),
            task.getWorkflowActivityId(), task.getTaskActivityId(), task.getTaskName(),
            task.getTaskId(), task.getLabels(), task.getImage(), task.getCommand(), task.getScript(), task.getArguments(), task.getParameters(),
            task.getEnvs(), task.getResults(), task.getWorkingDir(), task.getConfiguration(), task.getWorkspaces(),
            waitUntilTimeout, getTaskTimeout(task.getConfiguration()));
        results = tektonService.watchTask(task.getWorkflowId(), task.getWorkflowActivityId(),
            task.getTaskId(), task.getTaskActivityId(), task.getLabels(), getTaskTimeout(task.getConfiguration()));
        if (getTaskDeletionConfig(task.getConfiguration()).equals(TaskDeletionEnum.OnSuccess)) {
          // This will only delete on success as failure throws an Exception.
          deleteService.deleteJob(task.getWorkflowId(),
              task.getWorkflowActivityId(), task.getTaskId(), task.getTaskActivityId(),
              task.getLabels());
        }
      } catch (KubernetesClientException e) {
        // KubernetesClientException handles the case where an internal admission
        // controller rejects the creation
        if (e.getMessage().contains("admission webhook")) {
          LOGGER.info(e.toString());
          throw new BoomerangException(1, "ADMISSION_WEBHOOK_DENIED", HttpStatus.BAD_REQUEST, e.getMessage());
        } else {
          throw new BoomerangException(e, 1, e.toString(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
      } catch (KubeRuntimeException e) {
        LOGGER.info("DEBUG::Task Is Being Set as Failed");
        throw new BoomerangException(e, 1, e.toString(), HttpStatus.INTERNAL_SERVER_ERROR);
      } catch (InterruptedException e) {
        throw new BoomerangException(1, "TASK_CREATION_ERROR", HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
      } catch (ParseException e) {
        throw new BoomerangException(1, "TASK_CREATION_TIMEOUT_ERROR", HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
      } finally {
        response.setResults(results);
        kubeService.deleteTaskConfigMap(task.getWorkflowId(), task.getWorkflowActivityId(),
            task.getTaskId(), task.getTaskActivityId(), task.getLabels());
        if (getTaskDeletionConfig(task.getConfiguration()).equals(TaskDeletionEnum.Always)) {
          deleteService.deleteJob(task.getWorkflowId(),
              task.getWorkflowActivityId(), task.getTaskId(), task.getTaskActivityId(),
              task.getLabels());
        }
        LOGGER
            .info("Task (" + task.getTaskId() + ") has completed with code " + response.getCode());
      }
    }
    return response;
  }

   private TaskResponse executeTaskCustom(TaskCustom task) {
     TaskResponse response = new TaskResponse("100", "Tekton Custom Task has not yet been implemented. Support will come in a future release.", null);
     return response;
   }
}
