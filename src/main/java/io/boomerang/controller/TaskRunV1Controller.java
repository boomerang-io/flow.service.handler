package io.boomerang.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import io.boomerang.model.Task;
import io.boomerang.model.TaskResponse;
import io.boomerang.service.TaskService;

@RestController
@RequestMapping("/controller/api/v1/taskrun")
public class TaskRunV1Controller {

  @Autowired
  private TaskService controllerService;

  @PostMapping(value = "/execute")
  public TaskResponse executeTask(@RequestBody Task task) {
    return controllerService.executeTask(task);
  }

  @PostMapping(value = "/terminate")
  public TaskResponse terminateTask(@RequestBody Task task) {
    return controllerService.terminateTask(task);
  }
}
