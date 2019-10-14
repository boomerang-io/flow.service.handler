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
import net.boomerangplatform.kube.service.FlowKubeServiceImpl;
import net.boomerangplatform.model.CustomTask;
import net.boomerangplatform.model.Response;
import net.boomerangplatform.model.Task;
import net.boomerangplatform.model.TaskResponse;
import net.boomerangplatform.model.Workflow;

@Service
@Profile({"live", "local"})
public class FlowControllerServiceImpl implements ControllerService {

  private static final String EXCEPTION = "Exception: ";

  private static final Logger LOGGER = LogManager.getLogger(FlowControllerServiceImpl.class);

  @Autowired
  private FlowKubeServiceImpl kubeService;

  @Override
  public Response createWorkflow(Workflow workflow) {
    Response response = new Response("0", "Workflow Activity (" + workflow.getWorkflowActivityId()
        + ") has been created successfully.");
    try {
    	LOGGER.info(workflow.toString());
      if (workflow.getWorkflowStorage().getEnable()) {
        kubeService.createPVC(workflow.getWorkflowName(), workflow.getWorkflowId(),
            workflow.getWorkflowActivityId(), workflow.getWorkflowStorage().getSize());
        kubeService.watchPVC(workflow.getWorkflowId(), workflow.getWorkflowActivityId()).getPhase();
      }
      kubeService.createWorkflowConfigMap(workflow.getWorkflowName(), workflow.getWorkflowId(),
          workflow.getWorkflowActivityId(), workflow.getProperties());
      kubeService.watchConfigMap(workflow.getWorkflowId(), workflow.getWorkflowActivityId(), null);
    } catch (ApiException | KubeRuntimeException e) {
      LOGGER.error(EXCEPTION, e);
      response.setCode("1");
      response.setMessage(e.toString());
    }
    return response;
  }

  @Override
  public Response terminateWorkflow(Workflow workflow) {
    Response response = new Response("0", "Workflow Activity (" + workflow.getWorkflowActivityId()
        + ") has been terminated successfully.");
    try {
      kubeService.deletePVC(workflow.getWorkflowId(), workflow.getWorkflowActivityId());
      kubeService.deleteConfigMap(workflow.getWorkflowId(), workflow.getWorkflowActivityId(), null);
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
      kubeService.createTaskConfigMap(task.getWorkflowName(), task.getWorkflowId(),
          task.getWorkflowActivityId(), task.getTaskName(), task.getTaskId(),
          task.getProperties());
      kubeService.watchConfigMap(null, task.getWorkflowActivityId(), task.getTaskId());
      kubeService.createJob(task.getWorkflowName(), task.getWorkflowId(),
          task.getWorkflowActivityId(), task.getTaskName(), task.getTaskId(), task.getArguments(),
          task.getProperties());
      kubeService.watchJob(task.getWorkflowId(), task.getWorkflowActivityId(), task.getTaskId());
    } catch (KubeRuntimeException e) {
      LOGGER.error(EXCEPTION, e);
      response.setCode("1");
      response.setMessage(e.toString());
    } finally {
      response.setOutput(kubeService.getTaskOutPutConfigMapData(task.getWorkflowId(),
          task.getWorkflowActivityId(), task.getTaskId(), task.getTaskName()));
      kubeService.deleteConfigMap(null, task.getWorkflowActivityId(), task.getTaskId());
    }
    return response;
  }

  @Override
  public TaskResponse executeTask(CustomTask task) {
    TaskResponse response = new TaskResponse("0",
        "Task (" + task.getTaskId() + ") has been executed successfully.", null);
    try {
      kubeService.createTaskConfigMap(task.getWorkflowName(), task.getWorkflowId(),
          task.getWorkflowActivityId(), task.getTaskName(), task.getTaskId(),
          task.getProperties());
      kubeService.watchConfigMap(null, task.getWorkflowActivityId(), task.getTaskId());
      kubeService.createJob(task.getWorkflowName(), task.getWorkflowId(),
          task.getWorkflowActivityId(), task.getTaskName(), task.getTaskId(), task.getArguments(),
          task.getProperties(), task.getImage(), task.getCommand());
      kubeService.watchJob(task.getWorkflowId(), task.getWorkflowActivityId(), task.getTaskId());
    } catch (KubeRuntimeException e) {
      LOGGER.error(EXCEPTION, e);
      response.setCode("1");
      response.setMessage(e.toString());
    } finally {
      response.setOutput(kubeService.getTaskOutPutConfigMapData(task.getWorkflowId(),
          task.getWorkflowActivityId(), task.getTaskId(), task.getTaskName()));
      kubeService.deleteConfigMap(null, task.getWorkflowActivityId(), task.getTaskId());
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
