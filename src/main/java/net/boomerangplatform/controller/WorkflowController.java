package net.boomerangplatform.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import net.boomerangplatform.model.Workflow;
import net.boomerangplatform.service.ControllerService;

@RestController
@RequestMapping("/controller/workflow")
public class WorkflowController {
    
    @Autowired
    private ControllerService controllerService;
    
    @RequestMapping(value = "/create", method = RequestMethod.POST)
    public String createWorkflow(@RequestBody Workflow workflow) {
    	return controllerService.createWorkflow(workflow);
    }
    
    @RequestMapping(value = "/terminate", method = RequestMethod.POST)
    public String terminateFlow(@RequestBody Workflow workflow) {
    	return controllerService.terminateWorkflow(workflow);
    }

}