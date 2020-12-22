package net.boomerangplatform.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import net.boomerangplatform.model.Response;
import net.boomerangplatform.model.Workflow;
import net.boomerangplatform.service.WorkflowService;

@RestController
@RequestMapping("/controller/workflow")
public class WorkflowController {

  @Autowired
  private WorkflowService workflowService;

  @PostMapping(value = "/create")
  public Response createWorkflow(@RequestBody Workflow workflow) {
    return workflowService.createWorkflow(workflow);
  }

  @PostMapping(value = "/terminate")
  public Response terminateFlow(@RequestBody Workflow workflow) {
    return workflowService.terminateWorkflow(workflow);
  }
}
