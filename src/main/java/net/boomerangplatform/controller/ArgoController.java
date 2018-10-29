package net.boomerangplatform.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import net.boomerangplatform.model.glen.Workflow;
import net.boomerangplatform.service.ArgoService;

@RestController
@RequestMapping("/argo")
public class ArgoController {

    @Autowired
    private ArgoService argoService;
    
    @RequestMapping(value = "/workflow/{name}", method = RequestMethod.GET)
    public Workflow getWorkflow(@PathVariable String name) {
        return argoService.getWorkflow(name);
    }
    
    @RequestMapping(value = "/workflow", method = RequestMethod.POST)
    public Object createWorkflow(@RequestBody Workflow workflow) {
        return argoService.createWorkflow(workflow);
    }
}