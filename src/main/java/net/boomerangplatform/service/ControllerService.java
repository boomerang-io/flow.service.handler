package net.boomerangplatform.service;

import java.util.Map;
import net.boomerangplatform.model.Response;
import net.boomerangplatform.model.Task;
import net.boomerangplatform.model.TaskResponse;

public abstract interface ControllerService {

	TaskResponse executeTask(Task task);

	Response setJobOutputProperty(String workflowId, String workflowActivityId, String taskId, String taskName,
			String key, String value);

	Response setJobOutputProperties(String workflowId, String workflowActivityId, String taskId, String taskName,
			Map<String, String> properties);

    TaskResponse terminateTask(Task task);
}
