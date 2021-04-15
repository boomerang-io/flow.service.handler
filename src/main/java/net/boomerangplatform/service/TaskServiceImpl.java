package net.boomerangplatform.service;

import java.util.HashMap;
import java.util.Map;
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
import net.boomerangplatform.model.Response;
import net.boomerangplatform.model.Task;
import net.boomerangplatform.model.TaskConfiguration;
import net.boomerangplatform.model.TaskDeletionEnum;
import net.boomerangplatform.model.TaskResponse;
import net.boomerangplatform.model.TaskTemplate;

@Service
public class TaskServiceImpl implements TaskService {

  private static final Logger LOGGER = LogManager.getLogger(TaskServiceImpl.class);
  
  @Value("${kube.timeout.waitUntil}")
  protected long waitUntilTimeout;

  @Autowired
  private ConfigurationServiceImpl configurationService;

  @Autowired
  private NewKubeServiceImpl kubeService;

  @Autowired
  private TektonServiceImpl tektonService;

  @Autowired
  private DeleteServiceImpl deleteService;

  protected TaskDeletionEnum getTaskDeletion(TaskConfiguration taskConfiguration) {
    return taskConfiguration != null && taskConfiguration.getDeletion() != null
        ? taskConfiguration.getDeletion()
        : configurationService.getTaskDeletion();
  }

  protected Boolean isTaskDeletionNever(TaskConfiguration taskConfiguration) {
    return !TaskDeletionEnum.Never.equals(getTaskDeletion(taskConfiguration)) ? Boolean.TRUE
        : Boolean.FALSE;
  }

  @Override
  public TaskResponse terminateTask(Task task) {
    TaskResponse response = new TaskResponse("0",
        "Task (" + task.getTaskId() + ") is meant to be terminated now.", null);

    return response;
  }

  @Override
  public TaskResponse executeTask(Task task) {
    if (task instanceof TaskTemplate) {
      return executeTaskTemplate((TaskTemplate) task);
      // } else if (task instanceof TaskCustom) {
      // return executeTaskCustom((TaskCustom)task);
    } else {
      throw new BoomerangException(1, "UNKOWN_TASK_TYPE", HttpStatus.BAD_REQUEST,
          task.getClass().toString());
    }
  }

  private TaskResponse executeTaskTemplate(TaskTemplate task) {
    TaskResponse response = new TaskResponse("0",
        "Task (" + task.getTaskId() + ") has been executed successfully.", null);
    if (task.getImage() == null) {
      throw new BoomerangException(1, "NO_TASK_IMAGE", HttpStatus.BAD_REQUEST,
          task.getClass().toString());
    } else {
      try {
        kubeService.createTaskConfigMap(task.getWorkflowName(), task.getWorkflowId(),
            task.getWorkflowActivityId(), task.getTaskName(), task.getTaskId(),
            task.getTaskActivityId(), task.getLabels(), task.getParameters());
        boolean createLifecycleWatcher =
            task.getConfiguration().getLifecycle() ? Boolean.TRUE : Boolean.FALSE;
        String workspaceId = task.getWorkspaces() != null && task.getWorkspaces().get(0) != null
            ? task.getWorkspaces().get(0).getWorkspaceId()
            : null;
            tektonService.createTaskRun(createLifecycleWatcher, workspaceId, task.getWorkflowName(),
                task.getWorkflowId(), task.getWorkflowActivityId(), task.getTaskActivityId(),
                task.getTaskName(), task.getTaskId(), task.getLabels(), task.getArguments(),
                task.getParameters(), task.getImage(), task.getCommand(), task.getConfiguration(), waitUntilTimeout);
          tektonService.watchTask(task.getWorkflowId(), task.getWorkflowActivityId(), task.getTaskId(),
          task.getTaskActivityId(), task.getLabels());
//        kubeService.createJob(createLifecycleWatcher, workspaceId, task.getWorkflowName(),
//            task.getWorkflowId(), task.getWorkflowActivityId(), task.getTaskActivityId(),
//            task.getTaskName(), task.getTaskId(), task.getLabels(), task.getArguments(),
//            task.getParameters(), task.getImage(), task.getCommand(), task.getConfiguration(), waitUntilTimeout);
//        kubeService.watchJob(task.getWorkflowId(), task.getWorkflowActivityId(), task.getTaskId(),
//            task.getTaskActivityId(), task.getLabels());
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
        // Uses the workflowActivityId as the TaskOutput is stored in the Workflow Configmap
        // response.setResults(kubeService.getTaskOutPutConfigMapData(task.getWorkflowId(),
        // task.getWorkflowActivityId(), task.getTaskId(), task.getTaskName()));
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

  // private TaskResponse executeTaskCustom(TaskCustom task) {
  // TaskResponse response = new TaskResponse("0", "Task (" + task.getTaskId() + ") has been
  // executed successfully.",
  // null);
  // if (task.getImage() == null) {
  // throw new BoomerangException(1, "NO_TASK_IMAGE", HttpStatus.BAD_REQUEST,
  // task.getClass().toString());
  // } else {
  // try {
  // kubeService.createTaskConfigMap(task.getWorkflowName(), task.getWorkflowId(),
  // task.getWorkflowActivityId(), task.getTaskName(),
  // task.getTaskId(), task.getTaskActivityId(), task.getLabels(), task.getParameters());
  // kubeService.watchTaskConfigMap(task.getWorkflowId(), task.getWorkflowActivityId(),
  // task.getTaskId(), task.getTaskActivityId());
  // String workspaceId = task.getWorkspaces() != null && task.getWorkspaces().get(0) != null ?
  // task.getWorkspaces().get(0).getWorkspaceId() : null;
  // kubeService.createJob(true, workspaceId, task.getWorkflowName(), task.getWorkflowId(),
  // task.getWorkflowActivityId(),
  // task.getTaskActivityId(), task.getTaskName(), task.getTaskId(), task.getLabels(),
  // task.getArguments(),
  // task.getParameters(), task.getImage(), task.getCommand(), task.getConfiguration());
  // kubeService.watchJob(true, task.getWorkflowId(), task.getWorkflowActivityId(),
  // task.getTaskId(), task.getTaskActivityId());
  // } catch (KubeRuntimeException e) {
  // LOGGER.info("DEBUG::Task Is Being Set as Failed");
  // throw new BoomerangException(e, 1, e.toString(), HttpStatus.INTERNAL_SERVER_ERROR);
  // } finally {
  // response.setResults(kubeService.getTaskOutPutConfigMapData(task.getWorkflowId(),
  // task.getWorkflowActivityId(), task.getTaskId(), task.getTaskName()));
  // kubeService.deleteTaskConfigMap(task.getWorkflowId(), task.getWorkflowActivityId(),
  // task.getTaskId(), task.getTaskActivityId());
  // if (isTaskDeletionNever(task.getConfiguration())) {
  // deleteService.deleteJob(getTaskDeletion(task.getConfiguration()), task.getWorkflowId(),
  // task.getWorkflowActivityId(), task.getTaskId(), task.getTaskActivityId());
  // }
  // LOGGER.info("Task (" + task.getTaskId() + ") has completed with code " + response.getCode());
  // }
  // }
  // return response;
  // }

  @Override
  public Response setTaskResultParameter(String workflowId, String workflowActivityId,
      String taskId, String taskName, String key, String value) {
    Response response = new Response("0", "Parameter has been set against workflow ("
        + workflowActivityId + ") and task (" + taskId + ")");
    try {
      Map<String, String> parameters = new HashMap<>();
      parameters.put(key, value);
      // kubeService.patchTaskConfigMap(workflowId, workflowActivityId, taskId, taskName,
      // parameters);
    } catch (KubeRuntimeException e) {
      throw new BoomerangException(e, 1, e.toString(), HttpStatus.INTERNAL_SERVER_ERROR);
    }
    return response;
  }

  @Override
  public Response setTaskResultParameters(String workflowId, String workflowActivityId,
      String taskId, String taskName, Map<String, String> parameters) {
    Response response = new Response("0", "Parameters have been set against workflow ("
        + workflowActivityId + ") and task (" + taskId + ")");

    LOGGER.info(parameters);
    try {
      // kubeService.patchTaskConfigMap(workflowId, workflowActivityId, taskId, taskName,
      // parameters);
    } catch (KubeRuntimeException e) {
      throw new BoomerangException(e, 1, e.toString(), HttpStatus.INTERNAL_SERVER_ERROR);
    }
    return response;
  }
}
