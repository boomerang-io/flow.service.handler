package net.boomerangplatform.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import net.boomerangplatform.model.Task;

@Service
public class ControllerServiceImpl implements ControllerService {
	
	@Autowired
    private KubeService kubeService;

	@Override
	public String executeTask(Task task) {
		
		try {
			kubeService.createJob(task.getWorkflowName(),task.getWorkflowID());
			return kubeService.watchJob(task.getWorkflowName());
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return e.toString();
		}
	}
}
