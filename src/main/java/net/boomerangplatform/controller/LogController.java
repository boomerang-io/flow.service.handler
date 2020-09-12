package net.boomerangplatform.controller;

import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import net.boomerangplatform.service.LogService;

@RestController
@RequestMapping("/controller/log")
public class LogController {

  @Autowired
  private LogService logService;

//  @GetMapping(value = "/get")
//  public Response getLogForTask(
//      @RequestParam(value = "workflowId", required = true) String workflowId,
//      @RequestParam(value = "workflowActivityId", required = true) String workflowActivityId,
//      @RequestParam(value = "taskActivityId", required = false) String taskActivityId,
//      @RequestParam(value = "taskId", required = true) String taskId) {
//    return logService.getLogForTask(workflowId, workflowActivityId, taskId,taskActivityId);
//  }

  @GetMapping(value = "/stream")
  public ResponseEntity<StreamingResponseBody> streamLogForTask(HttpServletResponse response,
      @RequestParam(value = "workflowId", required = true) String workflowId,
      @RequestParam(value = "workflowActivityId", required = true) String workflowActivityId,
      @RequestParam(value = "taskActivityId", required = false) String taskActivityId,
      @RequestParam(value = "taskId", required = true) String taskId) {
    return new ResponseEntity<>(
        logService.streamLogForTask(response, workflowId, workflowActivityId, taskId, taskActivityId),
        HttpStatus.OK);
  }
}
