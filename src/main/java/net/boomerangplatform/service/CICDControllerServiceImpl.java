package net.boomerangplatform.service;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import net.boomerangplatform.kube.service.CICDKubeServiceImpl;
import net.boomerangplatform.model.Response;
import net.boomerangplatform.model.Task;
import net.boomerangplatform.model.TaskResponse;
import net.boomerangplatform.model.Workflow;

@Service
@Profile("cicd")
public class CICDControllerServiceImpl implements ControllerService {
	
	@Autowired
    private CICDKubeServiceImpl kubeService;
	
	@Override
	public Response createWorkflow(Workflow workflow) {
		Response response = new Response("0","Component Activity (" + workflow.getWorkflowActivityId() + ") has been created successfully.");
		try {
			if (workflow.getWorkflowStorage().getEnable()) {
				kubeService.createPVC(workflow.getWorkflowName(), workflow.getWorkflowId(), workflow.getWorkflowActivityId(), null);
				kubeService.watchPVC(workflow.getWorkflowId(), workflow.getWorkflowActivityId()).getPhase();
			}
		} catch (Exception e) {
			e.printStackTrace();
			response.setCode("1");
			response.setMessage(e.toString());
		}
		return response;
	}
	
	@Override
	public Response terminateWorkflow(Workflow workflow) {
		Response response = new Response("0","Component Storage (" + workflow.getWorkflowId() + ") has been removed successfully.");
		try {
			kubeService.deletePVC(workflow.getWorkflowId(), null);
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
			//TODO separate out cache handling to separate try catch to handle failure and continue.
			boolean cacheEnable = task.getInputs().getProperties().get("component/cache.enabled").equals("true");
			boolean cacheExists = kubeService.checkPVCExists(task.getWorkflowId(), null, null, true);
			if (cacheEnable && !cacheExists) {
				kubeService.createPVC(task.getWorkflowName(), task.getWorkflowId(), task.getWorkflowActivityId(), null);
				kubeService.watchPVC(task.getWorkflowId(), task.getWorkflowActivityId());
			} else if (!cacheEnable && cacheExists) {
				kubeService.deletePVC(task.getWorkflowId(), null);
			}
			kubeService.createTaskConfigMap(task.getWorkflowName(), task.getWorkflowId(), task.getWorkflowActivityId(), task.getTaskName(), task.getTaskId(), task.getInputs().getProperties());
			kubeService.watchConfigMap(task.getWorkflowId(), task.getWorkflowActivityId(), task.getTaskId());
			kubeService.createJob(task.getWorkflowName(), task.getWorkflowId(), task.getWorkflowActivityId(), task.getTaskName(), task.getTaskId(), task.getArguments(), task.getInputs().getProperties());
			kubeService.watchJob(task.getWorkflowId(), task.getWorkflowActivityId(), task.getTaskId());
		} catch (Exception e) {
			e.printStackTrace();
			response.setCode("1");
			response.setMessage(e.toString());
		} finally {
//			response.setOutput(kubeService.getTaskOutPutConfigMapData(task.getWorkflowId(), task.getWorkflowActivityId(), task.getTaskId(), task.getTaskName()));
			kubeService.deleteConfigMap(task.getWorkflowId(), task.getWorkflowActivityId(), task.getTaskId());
		}
		return response;
	}
	
	@Override
	public Response setJobOutputProperty(String workflowId, String workflowActivityId, String taskId, String taskName, String key, String value) {
		Response response = new Response("0","Property has been set against workflow (" + workflowActivityId + ") and task (" + taskId + ")");	
		try {
			Map<String, String> properties = new HashMap<String, String>();
			properties.put(key, value);
			kubeService.patchTaskConfigMap(workflowId, workflowActivityId, taskId, taskName, properties);
		} catch (Exception e) {
			e.printStackTrace();
			response.setCode("1");
			response.setMessage(e.toString());
		} 
		return response;
	}
	
	@Override
	public Response setJobOutputProperties(String workflowId, String workflowActivityId, String taskId, String taskName, Map<String, String> properties) {
		Response response = new Response("0","Properties have been set against workflow (" + workflowActivityId + ") and task (" + taskId + ")");
		
		System.out.println(properties);
		try {
			kubeService.patchTaskConfigMap(workflowId, workflowActivityId, taskId, taskName, properties);
		} catch (Exception e) {
			e.printStackTrace();
			response.setCode("1");
			response.setMessage(e.toString());
		} 
		return response;
	}
	
	@Override
	public Response getLogForTask(String workflowId, String workflowActivityId, String taskId) {
		Response response = new Response("0","");
		try {
			response.setMessage(kubeService.getPodLog(workflowId, workflowActivityId, taskId));
		} catch (Exception e) {
			e.printStackTrace();
			response.setCode("1");
			response.setMessage(e.toString());
		} 
		return response;
	}
	
	@Override
	public StreamingResponseBody streamLogForTask(HttpServletResponse response, String workflowId, String workflowActivityId, String taskId) {
		StreamingResponseBody srb = null;
		try {
			srb = kubeService.streamPodLog(response, workflowId, workflowActivityId, taskId);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return srb; 
	}
}
