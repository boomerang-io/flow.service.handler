package net.boomerangplatform.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import net.boomerangplatform.model.Task;
import net.boomerangplatform.model.TaskResponse;
import net.boomerangplatform.service.AbstractControllerService;

@RestController
@RequestMapping("/controller/task")
public class TaskController {

  @Autowired
  private AbstractControllerService controllerService;

  @PostMapping(value = "/execute")
  public TaskResponse executeTask(@RequestBody Task task) {
    return controllerService.executeTask(task);
  }
}
