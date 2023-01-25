package io.boomerang.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import io.boomerang.model.TaskRequest;
import io.boomerang.model.TaskResponse;
import io.boomerang.service.TaskService;

@RestController
@RequestMapping("/api/v1/task/run")
public class TaskRunV1Controller {

  @Autowired
  private TaskService taskService;

  @Deprecated
  @PostMapping(value = "/execute")
  public TaskResponse executeTask(@RequestBody TaskRequest task) {
    return taskService.execute(task);
  }

  @Deprecated
  @PostMapping(value = "/terminate")
  public TaskResponse terminateTask(@RequestBody TaskRequest task) {
    return taskService.terminate(task);
  }
}
