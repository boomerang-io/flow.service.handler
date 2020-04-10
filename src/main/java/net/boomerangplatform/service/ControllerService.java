package net.boomerangplatform.service;

import java.util.Map;

import javax.servlet.http.HttpServletResponse;

import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

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

	Response getLogForTask(String workflowId, String workflowActivityId, String taskId,  String taskActivityId);

	StreamingResponseBody streamLogForTask(HttpServletResponse response, String workflowId, String workflowActivityId,
			String taskId,  String taskActivityId);

	Response setJobOutputProperties(String workflowId, String workflowActivityId, String taskId, String taskName,
			Map<String, String> properties);
}
