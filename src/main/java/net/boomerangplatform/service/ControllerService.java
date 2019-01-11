package net.boomerangplatform.service;

import net.boomerangplatform.model.Response;
import net.boomerangplatform.model.Task;
import net.boomerangplatform.model.TaskResponse;
import net.boomerangplatform.model.Workflow;

public interface ControllerService {

	TaskResponse executeTask(Task task);
	Response createWorkflow(Workflow workflow);
	Response terminateWorkflow(Workflow workflow);
	Response setJobOutputProperty(String workflowId, String workflowActivityId, String taskId, String taskName,
			String key, String value);
}