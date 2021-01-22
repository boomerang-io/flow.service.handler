package net.boomerangplatform.service;

import java.util.HashMap;
import java.util.Map;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import net.boomerangplatform.error.BoomerangException;
import net.boomerangplatform.kube.exception.KubeRuntimeException;
import net.boomerangplatform.kube.service.KubeServiceImpl;
import net.boomerangplatform.model.Response;
import net.boomerangplatform.model.Task;
import net.boomerangplatform.model.TaskConfiguration;
import net.boomerangplatform.model.TaskCustom;
import net.boomerangplatform.model.TaskDeletionEnum;
import net.boomerangplatform.model.TaskResponse;
import net.boomerangplatform.model.TaskTemplate;

@Service
public class TaskServiceImpl implements TaskService {

  private static final Logger LOGGER = LogManager.getLogger(TaskServiceImpl.class);

	@Autowired
	private ConfigurationServiceImpl configurationService;

    @Autowired
    private KubeServiceImpl kubeService;

    @Autowired
    private DeleteServiceImpl deleteService;

	protected TaskDeletionEnum getTaskDeletion(TaskConfiguration taskConfiguration) {
      return taskConfiguration != null && taskConfiguration.getDeletion() != null ? taskConfiguration.getDeletion()
          : configurationService.getTaskDeletion();
	}
	
	protected Boolean isTaskDeletionNever(TaskConfiguration taskConfiguration) {
		return !TaskDeletionEnum.Never.equals(getTaskDeletion(taskConfiguration)) ? Boolean.TRUE : Boolean.FALSE;
	}
    
    @Override
    public TaskResponse terminateTask(Task task) {
      TaskResponse response = new TaskResponse("0", "Task (" + task.getTaskId() + ") is meant to be terminated now.",
              null);

      return response;
    }    

    @Override
    public TaskResponse executeTask(Task task) {
        if (task instanceof TaskTemplate) {
            return executeTaskTemplate((TaskTemplate)task);
        } else if (task instanceof TaskCustom) {
            return executeTaskCustom((TaskCustom)task);
        } else {
            throw new BoomerangException(1,"UNKOWN_TASK_TYPE",HttpStatus.BAD_REQUEST, task.getClass().toString());
        }
    }

      private TaskResponse executeTaskTemplate(TaskTemplate task) {
          TaskResponse response = new TaskResponse("0", "Task (" + task.getTaskId() + ") has been executed successfully.",
                  null);
          if (task.getImage() == null) {
              throw new BoomerangException(1, "NO_TASK_IMAGE", HttpStatus.BAD_REQUEST, task.getClass().toString());
          } else {
              try {
                  kubeService.createTaskConfigMap(task.getWorkflowName(), task.getWorkflowId(),
                          task.getTaskActivityId(), task.getTaskName(), task.getTaskId(), task.getParameters());
                  kubeService.watchConfigMap(null, task.getTaskActivityId(), task.getTaskId());
                  boolean createWatchLifecycle = task.getArguments().contains("shell") ? Boolean.TRUE : Boolean.FALSE;
                  String workspaceId = task.getWorkspaces() != null && task.getWorkspaces().get(0) != null ? task.getWorkspaces().get(0).getWorkspaceId() : null;
                  kubeService.createJob(createWatchLifecycle, workspaceId, task.getWorkflowName(), task.getWorkflowId(),
                          task.getWorkflowActivityId(), task.getTaskActivityId(), task.getTaskName(), task.getTaskId(),
                          task.getArguments(), task.getParameters(), task.getImage(), task.getCommand(),
                          task.getConfiguration());
                  kubeService.watchJob(createWatchLifecycle, task.getWorkflowId(), task.getTaskActivityId(),
                          task.getTaskId());
              } catch (KubeRuntimeException e) {
                  LOGGER.info("DEBUG::Task Is Being Set as Failed");
                    throw new BoomerangException(e, 1, e.toString(), HttpStatus.INTERNAL_SERVER_ERROR);
              } finally {
//                Uses the workflowActivityId as the TaskOutput is stored in the Workflow Configmap
                  response.setResults(kubeService.getTaskOutPutConfigMapData(task.getWorkflowId(),
                          task.getWorkflowActivityId(), task.getTaskId(), task.getTaskName()));
                  kubeService.deleteConfigMap(null, task.getTaskActivityId(), task.getTaskId());
                  if (isTaskDeletionNever(task.getConfiguration())) {
                    deleteService.deleteJob(getTaskDeletion(task.getConfiguration()), task.getWorkflowId(),
                              task.getTaskActivityId(), task.getTaskId());
                  }
                  LOGGER.info("Task (" + task.getTaskId() + ") has completed with code " + response.getCode());
              }
          }
          return response;
      }

    private TaskResponse executeTaskCustom(TaskCustom task) {
          TaskResponse response = new TaskResponse("0", "Task (" + task.getTaskId() + ") has been executed successfully.",
                  null);
          if (task.getImage() == null) {
              throw new BoomerangException(1, "NO_TASK_IMAGE", HttpStatus.BAD_REQUEST, task.getClass().toString());
          } else {
              try {
                  kubeService.createTaskConfigMap(task.getWorkflowName(), task.getWorkflowId(),
                          task.getTaskActivityId(), task.getTaskName(), task.getTaskId(), task.getParameters());
                  kubeService.watchConfigMap(null, task.getTaskActivityId(), task.getTaskId());
                  String workspaceId = task.getWorkspaces() != null && task.getWorkspaces().get(0) != null ? task.getWorkspaces().get(0).getWorkspaceId() : null;
                  kubeService.createJob(true, workspaceId, task.getWorkflowName(), task.getWorkflowId(), task.getWorkflowActivityId(),
                          task.getTaskActivityId(), task.getTaskName(), task.getTaskId(), task.getArguments(),
                          task.getParameters(), task.getImage(), task.getCommand(), task.getConfiguration());
                  kubeService.watchJob(true, task.getWorkflowId(), task.getWorkflowActivityId(), task.getTaskId());
              } catch (KubeRuntimeException e) {
                  LOGGER.info("DEBUG::Task Is Being Set as Failed");
                  throw new BoomerangException(e, 1, e.toString(), HttpStatus.INTERNAL_SERVER_ERROR);
              } finally {
                  response.setResults(kubeService.getTaskOutPutConfigMapData(task.getWorkflowId(),
                          task.getWorkflowActivityId(), task.getTaskId(), task.getTaskName()));
                  kubeService.deleteConfigMap(null, task.getTaskActivityId(), task.getTaskId());
                  if (isTaskDeletionNever(task.getConfiguration())) {
                    deleteService.deleteJob(getTaskDeletion(task.getConfiguration()), task.getWorkflowId(),
                        task.getTaskActivityId(), task.getTaskId());
                  }
                  LOGGER.info("Task (" + task.getTaskId() + ") has completed with code " + response.getCode());
              }
          }
          return response;
    }

    @Override
    public Response setTaskResultParameter(String workflowId, String workflowActivityId, String taskId,
        String taskName, String key, String value) {
      Response response = new Response("0", "Parameter has been set against workflow ("
          + workflowActivityId + ") and task (" + taskId + ")");
      try {
        Map<String, String> parameters = new HashMap<>();
        parameters.put(key, value);
        kubeService.patchTaskConfigMap(workflowId, workflowActivityId, taskId, taskName, parameters);
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
        kubeService.patchTaskConfigMap(workflowId, workflowActivityId, taskId, taskName, parameters);
      } catch (KubeRuntimeException e) {
        throw new BoomerangException(e, 1, e.toString(), HttpStatus.INTERNAL_SERVER_ERROR);
      }
      return response;
    }
}
