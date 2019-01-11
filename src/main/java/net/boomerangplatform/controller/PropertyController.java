package net.boomerangplatform.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import net.boomerangplatform.model.Response;
import net.boomerangplatform.model.Task;
import net.boomerangplatform.model.TaskResponse;
import net.boomerangplatform.service.ControllerService;

@RestController
@RequestMapping("/controller/property")
public class PropertyController {
    
    @Autowired
    private ControllerService controllerService;
     @RequestMapping(value = "/set", method = RequestMethod.PUT)
    public Response setOutPutProperty(@RequestParam(value = "workflowId", required = true) String workflowId, @RequestParam(value = "workflowActivityId", required = true) String workflowActivityId, @RequestParam(value = "taskId", required = true) String taskId, @RequestParam(value = "taskName", required = true) String taskName, @RequestParam(value = "key", required = true) String key, @RequestParam(value = "value", required = true) String value) {
    	return controllerService.setJobOutputProperty(workflowId, workflowActivityId, taskId, taskName, key, value);
    }
}