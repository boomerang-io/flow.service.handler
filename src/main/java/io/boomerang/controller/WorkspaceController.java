package io.boomerang.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import io.boomerang.model.Response;
import io.boomerang.model.Workspace;
import io.boomerang.service.WorkspaceService;

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
