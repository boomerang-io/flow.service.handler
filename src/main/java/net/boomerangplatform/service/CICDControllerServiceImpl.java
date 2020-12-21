package net.boomerangplatform.service;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import io.kubernetes.client.ApiException;
import net.boomerangplatform.error.BoomerangException;
import net.boomerangplatform.kube.exception.KubeRuntimeException;
import net.boomerangplatform.kube.service.CICDKubeServiceImpl;
import net.boomerangplatform.model.Response;
import net.boomerangplatform.model.Task;
import net.boomerangplatform.model.TaskCICD;
import net.boomerangplatform.model.TaskResponse;
import net.boomerangplatform.model.Workflow;

@Service
@Profile("cicd")
public class CICDControllerServiceImpl extends AbstractControllerServiceImpl {

  private static final Logger LOGGER = LogManager.getLogger(CICDControllerServiceImpl.class);
  
  @Autowired
  private CICDKubeServiceImpl kubeService;

  @Autowired
  private DeleteServiceImpl deleteService;

  @Override
  public Response createWorkflow(Workflow workflow) {
    Response response = new Response("0", "Component Activity (" + workflow.getWorkflowActivityId()
        + ") has been created successfully.");
    try {
      if (workflow.getWorkflowStorage().getEnable()) {
        kubeService.createWorkflowPVC(workflow.getWorkflowName(), workflow.getWorkflowId(),
            workflow.getWorkflowActivityId(), null);
        kubeService.watchWorkflowPVC(workflow.getWorkflowId(), workflow.getWorkflowActivityId()).getPhase();
      }
    } catch (ApiException | KubeRuntimeException e) {
  	  throw new BoomerangException(e, 1, e.toString(), HttpStatus.INTERNAL_SERVER_ERROR);
    }
    return response;
  }

  @Override
  public Response terminateWorkflow(Workflow workflow) {
    Response response = new Response("0",
        "Component Storage (" + workflow.getWorkflowId() + ") has been removed successfully.");
    try {
      kubeService.deleteWorkflowPVC(workflow.getWorkflowId(), null);
    } catch (KubeRuntimeException e) {
	  throw new BoomerangException(e, 1, e.toString(), HttpStatus.INTERNAL_SERVER_ERROR);
    }
    return response;
  }

  @Override
  public TaskResponse executeTask(Task task) {
	  if (task instanceof TaskCICD) {
		  return executeTaskCICD((TaskCICD)task);
	  } else {
		  throw new BoomerangException(1,"UNKOWN_TASK_TYPE",HttpStatus.BAD_REQUEST, task.getClass().toString());
	  }
  }

	private TaskResponse executeTaskCICD(TaskCICD task) {
		TaskResponse response = new TaskResponse("0", "Task (" + task.getTaskId() + ") has been executed successfully.", null);
		if (task.getImage() == null) {
			  throw new BoomerangException(1,"NO_TASK_IMAGE",HttpStatus.BAD_REQUEST, task.getClass().toString());
		} else {
			try {
				kubeService.createTaskConfigMap(task.getWorkflowName(), task.getWorkflowId(),
						task.getWorkflowActivityId(), task.getTaskName(), task.getTaskId(), task.getProperties());
				kubeService.watchConfigMap(task.getWorkflowId(), task.getWorkflowActivityId(), task.getTaskId());
				kubeService.createJob(false, task.getWorkflowName(), task.getWorkflowId(), task.getWorkflowActivityId(),
						task.getTaskActivityId(), task.getTaskName(), task.getTaskId(), task.getArguments(),
						task.getProperties(), task.getImage(), task.getCommand(), task.getConfiguration());
				kubeService.watchJob(false, task.getWorkflowId(), task.getWorkflowActivityId(), task.getTaskId());
			} catch (KubeRuntimeException e) {
				LOGGER.info("DEBUG::Task Is Being Set as Failed");
				  throw new BoomerangException(e, 1, e.toString(), HttpStatus.INTERNAL_SERVER_ERROR);
			} finally {
				kubeService.deleteConfigMap(task.getWorkflowId(), task.getWorkflowActivityId(), task.getTaskId());
			      if (isTaskDeletionNever(task.getConfiguration().getDeletion())) {
			        deleteService.deleteJob(getTaskDeletion(task.getConfiguration().getDeletion()), task.getWorkflowId(),
                        task.getWorkflowActivityId(), task.getTaskId());
			        }
				LOGGER.info("Task (" + task.getTaskId() + ") has completed with code " + response.getCode());
			}
		}
		return response;
	}
}
