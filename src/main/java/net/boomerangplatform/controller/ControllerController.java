package net.boomerangplatform.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import io.kubernetes.client.models.V1Job;
import io.kubernetes.client.models.V1JobList;
import io.kubernetes.client.models.V1NamespaceList;
import net.boomerangplatform.model.Workflow;
import net.boomerangplatform.service.KubeService;

@RestController
@RequestMapping("/controller")
public class ControllerController {

    @Autowired
    private KubeService kubeService;

    @RequestMapping(value = "/namespace", method = RequestMethod.GET)
    public V1NamespaceList getAllNamespaces() {
        return kubeService.getAllNamespaces();
    }
    
    @RequestMapping(value = "/jobs", method = RequestMethod.GET)
    public V1JobList getAllJobs() {
        return kubeService.getAllJobs();
    }
    
    @RequestMapping(value = "/jobs/create", method = RequestMethod.GET)
    public V1Job createJob() {
        return kubeService.createJob();
    }
    
    @RequestMapping(value = "/workflow/{name}", method = RequestMethod.GET)
    public Workflow getWorkflow(@PathVariable String name) {
        return kubeService.getWorkflow(name);
    }
    
    @RequestMapping(value = "/workflow", method = RequestMethod.POST)
    public Object createWorkflow(@RequestBody Workflow workflow) {
        return kubeService.createWorkflow(workflow);
    }
}