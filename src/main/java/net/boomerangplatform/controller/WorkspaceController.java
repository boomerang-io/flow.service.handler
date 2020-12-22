package net.boomerangplatform.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import net.boomerangplatform.model.Response;
import net.boomerangplatform.model.Workspace;
import net.boomerangplatform.service.ControllerService;

@RestController
@RequestMapping("/controller/workspace")
public class WorkspaceController {

  @Autowired
  private ControllerService controllerService;

  @PostMapping(value = "/create")
  public Response createWorkspace(@RequestBody Workspace workspace) {
    return controllerService.createWorkspace(workspace);
  }

  @PostMapping(value = "/delete")
  public Response deleteWorkspace(@RequestBody Workspace workspace) {
    return controllerService.deleteWorkspace(workspace);
  }
}
