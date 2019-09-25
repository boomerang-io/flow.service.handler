package net.boomerangplatform.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import net.boomerangplatform.model.argo.Workflow;
import net.boomerangplatform.service.ArgoService;

@RestController
@RequestMapping("/argo")
public class ArgoController {

  @Autowired
  private ArgoService argoService;

  @GetMapping(value = "/workflow/{name}")
  public Workflow getWorkflow(@PathVariable String name) {
    return argoService.getWorkflow(name);
  }

  @PostMapping(value = "/workflow")
  public Object createWorkflow(@RequestBody Workflow workflow) {
    return argoService.createWorkflow(workflow);
  }
}
