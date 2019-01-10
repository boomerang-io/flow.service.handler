package net.boomerangplatform.service;

import net.boomerangplatform.model.Task;
import net.boomerangplatform.model.TaskResponse;
import net.boomerangplatform.model.Workflow;

public interface ControllerService {

	TaskResponse executeTask(Task task);
	String createWorkflow(Workflow workflow);
	String terminateWorkflow(Workflow workflow);
	String setJobOutputProperty(String jobId, String key, String value);
}