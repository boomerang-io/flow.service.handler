package net.boomerangplatform.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import net.boomerangplatform.model.Response;
import net.boomerangplatform.model.Task;
import net.boomerangplatform.model.TaskResponse;
import net.boomerangplatform.model.Workflow;

@Service
public class ControllerServiceImpl implements ControllerService {
	
	@Autowired
    private KubeService kubeService;
	
	@Override
	public Response createWorkflow(Workflow workflow) {
		Response response = new Response("0","Workflow Activity (" + workflow.getWorkflowActivityId() + ") has been created successfully.");
		try {
			if (workflow.getWorkflowStorage().getEnable()) {
				kubeService.createPVC(workflow.getWorkflowName(), workflow.getWorkflowId(), workflow.getWorkflowActivityId(), workflow.getWorkflowStorage().getSize());
				kubeService.watchPVC(workflow.getWorkflowId(), workflow.getWorkflowActivityId()).getPhase();
			}
			kubeService.createWorkflowConfigMap(workflow.getWorkflowName(), workflow.getWorkflowId(), workflow.getWorkflowActivityId(), workflow.getInputs());
			kubeService.watchConfigMap(workflow.getWorkflowId(), workflow.getWorkflowActivityId(), null);
		} catch (Exception e) {
			e.printStackTrace();
			response.setCode("1");
			response.setMessage(e.toString());
		}
		return response;
	}
	
	@Override
	public Response terminateWorkflow(Workflow workflow) {
		Response response = new Response("0","Workflow Activity (" + workflow.getWorkflowActivityId() + ") has been terminated successfully.");
		try {
			kubeService.deletePVC(workflow.getWorkflowId(), workflow.getWorkflowActivityId());
			kubeService.deleteConfigMap(workflow.getWorkflowId(), workflow.getWorkflowActivityId(), null);
		} catch (Exception e) {
			e.printStackTrace();
			response.setCode("1");
			response.setMessage(e.toString());
		}
		return response;
	}

	@Override
	public TaskResponse executeTask(Task task) {
		TaskResponse response = new TaskResponse("0","Task (" + task.getTaskId() + ") has been executed successfully.", null);
		try {
			kubeService.createTaskConfigMap(task.getWorkflowName(), task.getWorkflowId(), task.getWorkflowActivityId(), task.getTaskName(), task.getTaskId(), task.getInputs().getProperties());
			kubeService.watchConfigMap(task.getWorkflowId(), task.getWorkflowActivityId(), task.getTaskId());
			kubeService.createJob(task.getWorkflowName(), task.getWorkflowId(), task.getWorkflowActivityId(), task.getTaskName(), task.getTaskId(), task.getArguments(), task.getInputs().getProperties());
			kubeService.watchJob(task.getWorkflowId(), task.getWorkflowActivityId(), task.getTaskId());
		} catch (Exception e) {
			e.printStackTrace();
			response.setCode("1");
			response.setMessage(e.toString());
		} finally {
			response.setOutput(kubeService.getTaskOutPutConfigMapData(task.getWorkflowId(), task.getWorkflowActivityId(), task.getTaskId(), task.getTaskName()));
			kubeService.deleteConfigMap(task.getWorkflowId(), task.getWorkflowActivityId(), task.getTaskId());
		}
		return response;
	}
	
	@Override
	public Response setJobOutputProperty(String workflowId, String workflowActivityId, String taskId, String taskName, String key, String value) {
		Response response = new Response("0","Property has been set against workflow (" + workflowActivityId + ") and task (" + taskId + ")");	
		try {
			kubeService.patchTaskConfigMap(workflowId, workflowActivityId, taskId, taskName, key, value);
		} catch (Exception e) {
			e.printStackTrace();
			response.setCode("1");
			response.setMessage(e.toString());
		} 
		return response;
	}
}
