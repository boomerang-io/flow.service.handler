package net.boomerangplatform.service;

import java.util.Map;

import net.boomerangplatform.model.Response;
import net.boomerangplatform.model.Task;
import net.boomerangplatform.model.TaskResponse;
import net.boomerangplatform.model.Workflow;
import net.boomerangplatform.model.Workspace;

public abstract interface AbstractControllerService {

	TaskResponse executeTask(Task task);

	Response createWorkflow(Workflow workflow);

	Response terminateWorkflow(Workflow workflow);

	Response setJobOutputProperty(String workflowId, String workflowActivityId, String taskId, String taskName,
			String key, String value);

	Response setJobOutputProperties(String workflowId, String workflowActivityId, String taskId, String taskName,
			Map<String, String> properties);

    Response createWorkspace(Workspace workspace);

    Response deleteWorkspace(Workspace workspace);

    TaskResponse terminateTask(Task task);
}
