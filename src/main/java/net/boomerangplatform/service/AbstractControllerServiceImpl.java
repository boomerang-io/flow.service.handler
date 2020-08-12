package net.boomerangplatform.service;

import java.util.Map;

import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import net.boomerangplatform.model.Response;
import net.boomerangplatform.model.Task;
import net.boomerangplatform.model.TaskDeletion;
import net.boomerangplatform.model.TaskResponse;
import net.boomerangplatform.model.Workflow;

public abstract class AbstractControllerServiceImpl implements AbstractControllerService {

	@Autowired
	private ConfigurationServiceImpl configurationService;

	@Override
	public abstract TaskResponse executeTask(Task task);

	@Override
	public abstract Response createWorkflow(Workflow workflow);

	@Override
	public abstract Response terminateWorkflow(Workflow workflow);

	@Override
	public abstract Response setJobOutputProperty(String workflowId, String workflowActivityId, String taskId, String taskName,
			String key, String value);

	@Override
	public abstract Response getLogForTask(String workflowId, String workflowActivityId, String taskId, String taskActivityId);

	@Override
	public abstract StreamingResponseBody streamLogForTask(HttpServletResponse response, String workflowId,
			String workflowActivityId, String taskId, String taskActivityId);

	@Override
	public abstract Response setJobOutputProperties(String workflowId, String workflowActivityId, String taskId, String taskName,
			Map<String, String> properties);	  

	protected TaskDeletion getTaskDeletion(TaskDeletion taskDeletion) {
		return taskDeletion != null ? taskDeletion : configurationService.getTaskDeletion();
	}
	
	protected Boolean isTaskDeletionNever(TaskDeletion taskDeletion) {
		return !TaskDeletion.Never.equals(getTaskDeletion(taskDeletion)) ? Boolean.TRUE : Boolean.FALSE;
	}

}
