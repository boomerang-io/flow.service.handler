package net.boomerangplatform.service;

import net.boomerangplatform.model.Response;
import net.boomerangplatform.model.Workspace;

public abstract interface WorkspaceService {

    Response createWorkspace(Workspace workspace);

    Response deleteWorkspace(Workspace workspace);
}
