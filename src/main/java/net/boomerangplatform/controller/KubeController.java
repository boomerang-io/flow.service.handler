package net.boomerangplatform.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import io.kubernetes.client.models.V1NamespaceList;
import net.boomerangplatform.model.Workflow;
import net.boomerangplatform.service.KubeService;

@RestController
@RequestMapping("/kube")
public class KubeController {

    @Autowired
    private KubeService kubeService;

    @RequestMapping(value = "/namespace", method = RequestMethod.GET)
    public V1NamespaceList getAllNamespaces() {
        return kubeService.getAllNamespaces();
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