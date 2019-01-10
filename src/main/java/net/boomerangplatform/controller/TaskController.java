package net.boomerangplatform.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import net.boomerangplatform.model.Task;
import net.boomerangplatform.model.TaskResponse;
import net.boomerangplatform.service.ControllerService;

@RestController
@RequestMapping("/controller/task")
public class TaskController {
    
    @Autowired
    private ControllerService controllerService;
    
    @RequestMapping(value = "/create", method = RequestMethod.POST)
    public TaskResponse executeTask(@RequestBody Task task) {
    	return controllerService.executeTask(task);
    }

    @RequestMapping(value = "/{taskId}/property/set", method = RequestMethod.PUT)
    public String setOutPutProperty(@PathVariable String taskId, @RequestParam(value = "key", required = true) String key, @RequestParam(value = "value", required = true) String value) {
    	return controllerService.setJobOutputProperty(taskId, key, value);
    }


}