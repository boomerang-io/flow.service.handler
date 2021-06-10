package net.boomerangplatform.service;

import java.util.ArrayList;
import java.util.List;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import io.fabric8.kubernetes.client.KubernetesClientException;
import net.boomerangplatform.error.BoomerangException;
import net.boomerangplatform.kube.exception.KubeRuntimeException;
import net.boomerangplatform.kube.service.NewKubeServiceImpl;
import net.boomerangplatform.kube.service.TektonServiceImpl;
import net.boomerangplatform.model.Task;
import net.boomerangplatform.model.TaskConfiguration;
import net.boomerangplatform.model.TaskCustom;
import net.boomerangplatform.model.TaskDeletionEnum;
import net.boomerangplatform.model.TaskResponse;
import net.boomerangplatform.model.TaskResponseResult;
import net.boomerangplatform.model.TaskTemplate;

@Service
public class TaskServiceImpl implements TaskService {

  private static final Logger LOGGER = LogManager.getLogger(TaskServiceImpl.class);
  
  @Value("${kube.timeout.waitUntil}")
  protected long waitUntilTimeout;

  @Value("${kube.worker.job.deletion}")
  private TaskDeletionEnum workerDeletion;

  @Autowired
  private NewKubeServiceImpl kubeService;

  @Autowired
  private TektonServiceImpl tektonService;

  @Autowired
  private DeleteServiceImpl deleteService;

  protected TaskDeletionEnum getTaskDeletion(TaskConfiguration taskConfiguration) {
    return taskConfiguration != null && taskConfiguration.getDeletion() != null
        ? taskConfiguration.getDeletion()
        : workerDeletion;
  }

  protected Boolean isTaskDeletionNever(TaskConfiguration taskConfiguration) {
    return !TaskDeletionEnum.Never.equals(getTaskDeletion(taskConfiguration)) ? Boolean.TRUE
        : Boolean.FALSE;
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
    List<TaskResponseResult> results = new ArrayList<>();
    if (task.getImage() == null) {
      throw new BoomerangException(1, "NO_TASK_IMAGE", HttpStatus.BAD_REQUEST,
          task.getClass().toString());
    } else {
      try {
        kubeService.createTaskConfigMap(task.getWorkflowName(), task.getWorkflowId(),
            task.getWorkflowActivityId(), task.getTaskName(), task.getTaskId(),
            task.getTaskActivityId(), task.getLabels(), task.getParameters());
        String workspaceId = task.getWorkspaces() != null && task.getWorkspaces().get(0) != null
            ? task.getWorkspaces().get(0).getWorkspaceId()
            : null;
        tektonService.createTaskRun(workspaceId, task.getWorkflowName(), task.getWorkflowId(),
            task.getWorkflowActivityId(), task.getTaskActivityId(), task.getTaskName(),
            task.getTaskId(), task.getLabels(), task.getArguments(), task.getParameters(),
            task.getEnvs(), task.getResults(), task.getImage(), task.getCommand(),
            task.getWorkingDir(), task.getConfiguration(), waitUntilTimeout);
        results = tektonService.watchTask(task.getWorkflowId(), task.getWorkflowActivityId(),
            task.getTaskId(), task.getTaskActivityId(), task.getLabels());
      } catch (KubernetesClientException e) {
        // KubernetesClientException handles the case where an internal admission
        // controller rejects the creation
        if (e.getMessage().contains("admission webhook")) {
          LOGGER.error(e);
          throw new BoomerangException(1, "ADMISSION_WEBHOOK_DENIED", HttpStatus.BAD_REQUEST, e.getMessage());
        } else {
          throw new BoomerangException(e, 1, e.toString(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
      } catch (KubeRuntimeException e) {
        LOGGER.info("DEBUG::Task Is Being Set as Failed");
        throw new BoomerangException(e, 1, e.toString(), HttpStatus.INTERNAL_SERVER_ERROR);
      } catch (InterruptedException e) {
        throw new BoomerangException(1, "JOB_CREATION_ERROR", HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
      } finally {
        response.setResults(results);
        kubeService.deleteTaskConfigMap(task.getWorkflowId(), task.getWorkflowActivityId(),
            task.getTaskId(), task.getTaskActivityId(), task.getLabels());
        if (isTaskDeletionNever(task.getConfiguration())) {
          deleteService.deleteJob(getTaskDeletion(task.getConfiguration()), task.getWorkflowId(),
              task.getWorkflowActivityId(), task.getTaskId(), task.getTaskActivityId(),
              task.getLabels());
          // TODO: re-implement the delete onFailure option
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
