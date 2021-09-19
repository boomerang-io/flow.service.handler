package io.boomerang.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import io.boomerang.model.Response;
import io.boomerang.model.Workflow;
import io.boomerang.service.WorkflowService;

@RestController
@RequestMapping("/controller/workflow")
public class WorkflowController {

  @Autowired
  private WorkflowService workflowService;

  @PostMapping(value = "/execute")
  public Response createWorkflow(@RequestBody Workflow workflow) {
    return workflowService.createWorkflow(workflow);
  }

  @PostMapping(value = "/terminate")
  public Response terminateFlow(@RequestBody Workflow workflow) {
    return workflowService.terminateWorkflow(workflow);
  }
}
