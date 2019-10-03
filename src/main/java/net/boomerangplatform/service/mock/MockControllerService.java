package net.boomerangplatform.service.mock;

import java.util.Map;
import javax.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;
import net.boomerangplatform.model.Response;
import net.boomerangplatform.model.Task;
import net.boomerangplatform.model.TaskResponse;
import net.boomerangplatform.model.Workflow;
import net.boomerangplatform.service.ControllerService;

@Service
@Profile("local")
public class MockControllerService implements ControllerService {

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
  public Response setJobOutputProperty(String workflowId, String workflowActivityId, String taskId,
      String taskName, String key, String value) {
    return null;
  }

  @Override
  public Response getLogForTask(String workflowId, String workflowActivityId, String taskId) {
    return null;
  }

  @Override
  public StreamingResponseBody streamLogForTask(HttpServletResponse response, String workflowId,
      String workflowActivityId, String taskId) {
    return null;
  }

  @Override
  public Response setJobOutputProperties(String workflowId, String workflowActivityId,
      String taskId, String taskName, Map<String, String> properties) {
    return null;
  }

}
