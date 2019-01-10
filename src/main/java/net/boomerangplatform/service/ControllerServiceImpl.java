package net.boomerangplatform.service;

import java.util.HashMap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import net.boomerangplatform.model.Task;
import net.boomerangplatform.model.TaskProperties;
import net.boomerangplatform.model.TaskResponse;
import net.boomerangplatform.model.Workflow;

@Service
public class ControllerServiceImpl implements ControllerService {
	
	@Autowired
    private KubeService kubeService;
	
	private static HashMap<String, HashMap<String, String>> jobOutputPropertyCache = new HashMap<String,HashMap<String, String>>();
	
	@Override
	public String createWorkflow(Workflow workflow) {
		try {
			if (workflow.getWorkflowStorage().getEnable()) {
				kubeService.createPVC(workflow.getWorkflowName(), workflow.getWorkflowId(), workflow.getWorkflowActivityId(), workflow.getWorkflowStorage().getSize());
				kubeService.watchPVC(workflow.getWorkflowId(), workflow.getWorkflowActivityId()).getPhase();
			}
			if (workflow.getInputs() != null && !workflow.getInputs().isEmpty()) {
				kubeService.createConfigMap(workflow.getWorkflowName(), workflow.getWorkflowId(), workflow.getWorkflowActivityId(), workflow.getInputs());
				kubeService.watchConfigMap(workflow.getWorkflowId(), workflow.getWorkflowActivityId());
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return e.toString();
		}
		return "success";
	}
	
	@Override
	public String terminateWorkflow(Workflow workflow) {
		try {
			kubeService.deletePVC(workflow.getWorkflowId(), workflow.getWorkflowActivityId());
			kubeService.deleteConfigMap(workflow.getWorkflowId(), workflow.getWorkflowActivityId());
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return e.toString();
		}
		return "success";
	}

	@Override
	public TaskResponse executeTask(Task task) {
		
		try {
			kubeService.createJob(task.getWorkflowName(), task.getWorkflowId(), task.getWorkflowActivityId(), task.getTaskId(), task.getArguments(), task.getInputs().getProperties());
			kubeService.watchJob(task.getWorkflowId(), task.getWorkflowActivityId(), task.getTaskId());
			TaskResponse response = new TaskResponse("0","", jobOutputPropertyCache.get(task.getTaskId()));
			return response;
		} catch (Exception e) {
			// TODO Auto-generated catch block
			TaskResponse response = new TaskResponse("1",e.toString(), jobOutputPropertyCache.get(task.getTaskId()));
			e.printStackTrace();
			return response;
		}
	}
	
	@Override
	public String setJobOutputProperty(String jobId, String key, String value) {
		HashMap<String, String> properties = new HashMap<String,String>();
		if (jobOutputPropertyCache.containsKey(jobId)) {
			properties = jobOutputPropertyCache.get(jobId);
		}
		properties.put(key, value);
		jobOutputPropertyCache.put(jobId, properties);
		
		return "success";
	}
}
