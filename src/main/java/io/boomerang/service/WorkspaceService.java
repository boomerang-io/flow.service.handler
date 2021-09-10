package io.boomerang.service;

import io.boomerang.model.Response;
import io.boomerang.model.Workspace;

public abstract interface WorkspaceService {

    Response createWorkspace(Workspace workspace);

    Response deleteWorkspace(Workspace workspace);
}
