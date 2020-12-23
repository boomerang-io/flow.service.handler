package net.boomerangplatform.controller;

import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import net.boomerangplatform.model.Response;
import net.boomerangplatform.service.ControllerService;

@RestController
@RequestMapping("/controller/results")
public class ResultsController {

  @Autowired
  private ControllerService controllerService;

  @PatchMapping(value = "/parameter/set")
  public Response setResultParameter(
      @RequestParam(value = "workflowId", required = true) String workflowId,
      @RequestParam(value = "workflowActivityId", required = true) String workflowActivityId,
      @RequestParam(value = "taskId", required = true) String taskId,
      @RequestParam(value = "taskName", required = true) String taskName,
      @RequestParam(value = "key", required = true) String key,
      @RequestParam(value = "value", required = true) String value) {
    return controllerService.setTaskResultParameter(workflowId, workflowActivityId, taskId, taskName,
        key, value);
  }

  @PatchMapping(value = "/parameters/set")
  public Response setResultParameters(
      @RequestParam(value = "workflowId", required = true) String workflowId,
      @RequestParam(value = "workflowActivityId", required = true) String workflowActivityId,
      @RequestParam(value = "taskId", required = true) String taskId,
      @RequestParam(value = "taskName", required = true) String taskName,
      @RequestBody Map<String, String> body) {
    return controllerService.setTaskResultParameters(workflowId, workflowActivityId, taskId,
        taskName, body);
  }
}
