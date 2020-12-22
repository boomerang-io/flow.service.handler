package net.boomerangplatform.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import net.boomerangplatform.model.Response;
import net.boomerangplatform.model.Workspace;
import net.boomerangplatform.service.WorkspaceService;

@RestController
@RequestMapping("/controller/workspace")
public class WorkspaceController {

  @Autowired
  private WorkspaceService workspaceService;

  @PostMapping(value = "/create")
  public Response createWorkspace(@RequestBody Workspace workspace) {
    return workspaceService.createWorkspace(workspace);
  }

  @PostMapping(value = "/delete")
  public Response deleteWorkspace(@RequestBody Workspace workspace) {
    return workspaceService.deleteWorkspace(workspace);
  }
}
