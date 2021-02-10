package net.boomerangplatform.controller;

import java.util.Date;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import net.boomerangplatform.kube.service.HelperKubeService;
import net.boomerangplatform.model.Response;
import net.boomerangplatform.model.Workflow;
import net.boomerangplatform.service.WorkflowService;

@RestController
@RequestMapping("/controller/workflow")
public class WorkflowController {

  private static final Logger LOGGER = LogManager.getLogger(WorkflowController.class);

  @Autowired
  private WorkflowService workflowService;

  @PostMapping(value = "/create")
  public Response createWorkflow(@RequestBody Workflow workflow) {
    LOGGER.info("Creating Workflow: {}", workflow.getWorkflowActivityId());
    
    Date startTime = new Date();
    Response response = workflowService.createWorkflow(workflow);
    logRequestTime("Create Workflow", startTime, new Date());
    return response;
  }

  @PostMapping(value = "/terminate")
  public Response terminateFlow(@RequestBody Workflow workflow) {
    LOGGER.info("Terminating Workflow: {}", workflow.getWorkflowActivityId());
    
    Date startTime = new Date();
    Response response = workflowService.terminateWorkflow(workflow);
    logRequestTime("Terminate Workflow", startTime, new Date());
    return response;
  }

  private void logRequestTime(String payloadName, Date start, Date end) {
    long diff = end.getTime() - start.getTime();
    LOGGER.info("Benchmark [Request Type]: {} - {} ms", payloadName, diff);
  }
}
