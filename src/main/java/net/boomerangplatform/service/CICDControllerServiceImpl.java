package net.boomerangplatform.service;

import java.util.HashMap;
import java.util.Map;
import javax.servlet.http.HttpServletResponse;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;
import io.kubernetes.client.ApiException;
import net.boomerangplatform.kube.exception.KubeRuntimeException;
import net.boomerangplatform.kube.service.CICDKubeServiceImpl;
import net.boomerangplatform.model.Response;
import net.boomerangplatform.model.Task;
import net.boomerangplatform.model.TaskResponse;
import net.boomerangplatform.model.Workflow;

@Service
@Profile("cicd")
public class CICDControllerServiceImpl implements ControllerService {

  private static final String EXCEPTION = "Exception: ";

  private static final Logger LOGGER = LogManager.getLogger(CICDControllerServiceImpl.class);

  @Autowired
  private CICDKubeServiceImpl kubeService;

  @Override
  public Response createWorkflow(Workflow workflow) {
    Response response = new Response("0", "Component Activity (" + workflow.getWorkflowActivityId()
        + ") has been created successfully.");
    try {
      if (workflow.getWorkflowStorage().getEnable()) {
        kubeService.createPVC(workflow.getWorkflowName(), workflow.getWorkflowId(),
            workflow.getWorkflowActivityId(), null);
        kubeService.watchPVC(workflow.getWorkflowId(), workflow.getWorkflowActivityId()).getPhase();
      }
    } catch (ApiException | KubeRuntimeException e) {
      LOGGER.error(EXCEPTION, e);
      response.setCode("1");
      response.setMessage(e.toString());
    }
    return response;
  }

  @Override
  public Response terminateWorkflow(Workflow workflow) {
    Response response = new Response("0",
        "Component Storage (" + workflow.getWorkflowId() + ") has been removed successfully.");
    try {
      kubeService.deletePVC(workflow.getWorkflowId(), null);
    } catch (KubeRuntimeException e) {
      LOGGER.error(EXCEPTION, e);
      response.setCode("1");
      response.setMessage(e.toString());
    }
    return response;
  }

  @Override
  public TaskResponse executeTask(Task task) {
    TaskResponse response = new TaskResponse("0",
        "Task (" + task.getTaskId() + ") has been executed successfully.", null);
    try {
      // TODO separate out cache handling to separate try catch to handle failure and continue.
      boolean cacheEnable =
          "true".equals(task.getProperties().get("component/cache.enabled"));
      boolean cacheExists = kubeService.checkPVCExists(task.getWorkflowId(), null, null, true);
      if (cacheEnable && !cacheExists) {
        kubeService.createPVC(task.getWorkflowName(), task.getWorkflowId(),
            task.getWorkflowActivityId(), null);
        kubeService.watchPVC(task.getWorkflowId(), task.getWorkflowActivityId());
      } else if (!cacheEnable && cacheExists) {
        kubeService.deletePVC(task.getWorkflowId(), null);
      }
      kubeService.createTaskConfigMap(task.getWorkflowName(), task.getWorkflowId(),
          task.getWorkflowActivityId(), task.getTaskName(), task.getTaskId(),
          task.getProperties());
      kubeService.watchConfigMap(task.getWorkflowId(), task.getWorkflowActivityId(),
          task.getTaskId());
      kubeService.createJob(task.getWorkflowName(), task.getWorkflowId(),
          task.getWorkflowActivityId(), task.getTaskName(), task.getTaskId(), task.getArguments(),
          task.getProperties());
      kubeService.watchJob(task.getWorkflowId(), task.getWorkflowActivityId(), task.getTaskId());
    } catch (ApiException | KubeRuntimeException e) {
      LOGGER.error(EXCEPTION, e);
      response.setCode("1");
      response.setMessage(e.toString());
      LOGGER.info("DEBUG::Task Is Being Set as Failed");
    } finally {
      kubeService.deleteConfigMap(task.getWorkflowId(), task.getWorkflowActivityId(),
          task.getTaskId());
    }
    return response;
  }

  @Override
  public Response setJobOutputProperty(String workflowId, String workflowActivityId, String taskId,
      String taskName, String key, String value) {
    Response response = new Response("0", "Property has been set against workflow ("
        + workflowActivityId + ") and task (" + taskId + ")");
    try {
      Map<String, String> properties = new HashMap<>();
      properties.put(key, value);
      kubeService.patchTaskConfigMap(workflowId, workflowActivityId, taskId, taskName, properties);
    } catch (KubeRuntimeException e) {
      LOGGER.error(EXCEPTION, e);
      response.setCode("1");
      response.setMessage(e.toString());
    }
    return response;
  }

  @Override
  public Response setJobOutputProperties(String workflowId, String workflowActivityId,
      String taskId, String taskName, Map<String, String> properties) {
    Response response = new Response("0", "Properties have been set against workflow ("
        + workflowActivityId + ") and task (" + taskId + ")");

    LOGGER.info(properties);
    try {
      kubeService.patchTaskConfigMap(workflowId, workflowActivityId, taskId, taskName, properties);
    } catch (KubeRuntimeException e) {
      LOGGER.error(EXCEPTION, e);
      response.setCode("1");
      response.setMessage(e.toString());
    }
    return response;
  }

  @Override
  public Response getLogForTask(String workflowId, String workflowActivityId, String taskId) {
    Response response = new Response("0", "");
    try {
      response.setMessage(kubeService.getPodLog(workflowId, workflowActivityId, taskId));
    } catch (KubeRuntimeException e) {
      LOGGER.error(EXCEPTION, e);
      response.setCode("1");
      response.setMessage(e.toString());
    }
    return response;
  }

  @Override
  public StreamingResponseBody streamLogForTask(HttpServletResponse response, String workflowId,
      String workflowActivityId, String taskId) {
    StreamingResponseBody srb = null;
    try {
      srb = kubeService.streamPodLog(response, workflowId, workflowActivityId, taskId);
    } catch (KubeRuntimeException e) {
      LOGGER.error(EXCEPTION, e);
    }
    return srb;
  }
}
