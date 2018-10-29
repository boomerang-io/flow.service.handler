package net.boomerangplatform.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import net.boomerangplatform.model.Task;
import net.boomerangplatform.service.ControllerService;
import net.boomerangplatform.service.KubeService;

@RestController
@RequestMapping("/controller/task")
public class TaskController {

    @Autowired
    private KubeService kubeService;
    
    @Autowired
    private ControllerService controllerService;
    
    @RequestMapping(value = "/create", method = RequestMethod.POST)
    public String executeTask(@RequestBody Task task) {
    	return controllerService.executeTask(task);
    }

}