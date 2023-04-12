package io.boomerang.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import io.boomerang.model.Response;
import io.boomerang.model.WorkflowRequest;
import io.boomerang.service.WorkflowService;

@RestController
@RequestMapping("/api/v1/workflow/run")
public class WorkflowRunV1Controller {

  @Autowired
  private WorkflowService workflowService;

  @Deprecated
  @PostMapping(value = "/execute")
  public Response create(@RequestBody WorkflowRequest workflow) {
    return workflowService.execute(workflow);
  }

  @Deprecated
  @PostMapping(value = "/terminate")
  public Response terminate(@RequestBody WorkflowRequest workflow) {
    return workflowService.terminate(workflow);
  }
}
