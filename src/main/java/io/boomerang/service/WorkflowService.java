package io.boomerang.service;

import io.boomerang.model.Response;
import io.boomerang.model.Workflow;

public abstract interface WorkflowService {

	Response createWorkflow(Workflow workflow);

	Response terminateWorkflow(Workflow workflow);
}
