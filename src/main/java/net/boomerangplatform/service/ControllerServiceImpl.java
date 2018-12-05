package net.boomerangplatform.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import net.boomerangplatform.model.Task;
import net.boomerangplatform.model.Workflow;

@Service
public class ControllerServiceImpl implements ControllerService {
	
	@Autowired
    private KubeService kubeService;

	@Override
	public String executeTask(Task task) {
		
		try {
			kubeService.createJob(task.getWorkflowName(), task.getWorkflowId(), task.getWorkflowActivityId(), task.getTaskId(), task.getArguments(), task.getInputs().getProperties());
			return kubeService.watchJob(task.getWorkflowId(), task.getWorkflowActivityId(), task.getTaskId());
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return e.toString();
		}
	}
	
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
}
