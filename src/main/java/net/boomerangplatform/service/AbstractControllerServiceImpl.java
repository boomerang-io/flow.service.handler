package net.boomerangplatform.service;

import java.util.HashMap;
import java.util.Map;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import io.kubernetes.client.ApiException;
import net.boomerangplatform.error.BoomerangException;
import net.boomerangplatform.kube.exception.KubeRuntimeException;
import net.boomerangplatform.kube.service.AbstractKubeServiceImpl;
import net.boomerangplatform.model.Response;
import net.boomerangplatform.model.Task;
import net.boomerangplatform.model.TaskDeletionEnum;
import net.boomerangplatform.model.TaskResponse;
import net.boomerangplatform.model.Workflow;
import net.boomerangplatform.model.Workspace;

public abstract class AbstractControllerServiceImpl implements AbstractControllerService {

  private static final Logger LOGGER = LogManager.getLogger(AbstractControllerServiceImpl.class);

	@Autowired
	private ConfigurationServiceImpl configurationService;

    @Autowired
    private AbstractKubeServiceImpl kubeService;

	@Override
	public abstract TaskResponse executeTask(Task task);

	@Override
	public abstract Response createWorkflow(Workflow workflow);

	@Override
	public abstract Response terminateWorkflow(Workflow workflow);	  

	protected TaskDeletionEnum getTaskDeletion(TaskDeletionEnum taskDeletion) {
		return taskDeletion != null ? taskDeletion : configurationService.getTaskDeletion();
	}
	
	protected Boolean isTaskDeletionNever(TaskDeletionEnum taskDeletion) {
		return !TaskDeletionEnum.Never.equals(getTaskDeletion(taskDeletion)) ? Boolean.TRUE : Boolean.FALSE;
	}
	
    @Override
    public Response createWorkspace(Workspace workspace) {
      Response response =
          new Response("0", "Workspace (" + workspace.getId() + ") PVC has been created successfully.");
      try {
        LOGGER.info("Workspace: " + workspace.toString());
        boolean cacheExists = kubeService.checkWorkspacePVCExists(workspace.getId(), false);
        if (workspace.getStorage().getEnable() && !cacheExists) {
          kubeService.createWorkspacePVC(workspace.getName(), workspace.getId(), workspace.getStorage().getSize());
          kubeService.watchWorkspacePVC(workspace.getId());
        } else if (cacheExists) {
          response = new Response("0", "Workspace (" + workspace.getId() + ") PVC already existed.");
        }
      } catch (ApiException | KubeRuntimeException e) {
        LOGGER.error(e.getMessage());
        throw new BoomerangException(e, 1, e.toString(), HttpStatus.INTERNAL_SERVER_ERROR);
      }
      LOGGER.info("createWorkspace() - " + response.getMessage());
      return response;
    }

    @Override
    public Response deleteWorkspace(Workspace workspace) {
      Response response =
          new Response("0", "Workspace (" + workspace.getId() + ") has been deleted successfully.");
      try {
        LOGGER.info(workspace.toString());
        kubeService.deleteWorkspacePVC(workspace.getId());
      } catch (KubeRuntimeException e) {
        throw new BoomerangException(e, 1, e.toString(), HttpStatus.INTERNAL_SERVER_ERROR);
      }
      LOGGER.info("createWorkspace() - " + response.getMessage());
      return response;
    }
    
    @Override
    public TaskResponse terminateTask(Task task) {
      TaskResponse response = new TaskResponse("0", "Task (" + task.getTaskId() + ") is meant to be terminated now.",
              null);

      return response;
    }

    @Override
    public Response setJobOutputProperty(String workflowId, String workflowActivityId, String taskId,
        String taskName, String key, String value) {
      Response response = new Response("0", "Property has been set against workflow ("
          + workflowActivityId + ") and task (" + taskId + ")");
      try {
        Map<String, String> properties = new HashMap<>();
        properties.put(key, value);
        kubeService.patchTaskConfigMap(workflowId, workflowActivityId, taskId, taskName, properties);
      } catch (KubeRuntimeException e) {
        throw new BoomerangException(e, 1, e.toString(), HttpStatus.INTERNAL_SERVER_ERROR);
      }
      return response;
    }

    @Override
    public Response setJobOutputProperties(String workflowId, String workflowActivityId,
        String taskId, String taskName, Map<String, String> properties) {
      Response response = new Response("0", "Properties have been set against workflow ("
          + workflowActivityId + ") and task (" + taskId + ")");

      LOGGER.info(properties);
      try {
        kubeService.patchTaskConfigMap(workflowId, workflowActivityId, taskId, taskName, properties);
      } catch (KubeRuntimeException e) {
        throw new BoomerangException(e, 1, e.toString(), HttpStatus.INTERNAL_SERVER_ERROR);
      }
      return response;
    }
}
