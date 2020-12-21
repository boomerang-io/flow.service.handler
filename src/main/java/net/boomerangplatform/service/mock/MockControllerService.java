package net.boomerangplatform.service.mock;

import java.util.Map;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import net.boomerangplatform.model.Response;
import net.boomerangplatform.model.Task;
import net.boomerangplatform.model.TaskResponse;
import net.boomerangplatform.model.Workflow;
import net.boomerangplatform.model.Workspace;
import net.boomerangplatform.service.AbstractControllerService;

@Service
@Profile("mock")
public class MockControllerService implements AbstractControllerService {

	@Override
	public TaskResponse executeTask(Task task) {
		return null;
	}

	@Override
	public Response createWorkflow(Workflow workflow) {
		return null;
	}

	@Override
	public Response terminateWorkflow(Workflow workflow) {
		return null;
	}

	@Override
	public Response setJobOutputProperty(String workflowId, String workflowActivityId, String taskId, String taskName,
			String key, String value) {
		return null;
	}

	@Override
	public Response setJobOutputProperties(String workflowId, String workflowActivityId, String taskId, String taskName,
			Map<String, String> properties) {
		return null;
	}

  @Override
  public Response createWorkspace(Workspace workspace) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public Response deleteWorkspace(Workspace workspace) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public TaskResponse terminateTask(Task task) {
    // TODO Auto-generated method stub
    return null;
  }

}
