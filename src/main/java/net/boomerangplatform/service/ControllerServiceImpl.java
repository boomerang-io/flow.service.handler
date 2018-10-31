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
			kubeService.createPVC(workflow.getWorkflowName(), workflow.getWorkflowId(), workflow.getWorkflowActivityId());
			return kubeService.watchPVC(workflow.getWorkflowId(), workflow.getWorkflowActivityId()).getPhase();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return e.toString();
		}
	}
	
	@Override
	public String terminateWorkflow(Workflow workflow) {
		
		try {
			kubeService.deletePVC(workflow.getWorkflowId(), workflow.getWorkflowActivityId());
			return "success";
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return e.toString();
		}
	}
}
