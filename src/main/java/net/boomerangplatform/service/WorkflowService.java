package net.boomerangplatform.service;

import net.boomerangplatform.model.Response;
import net.boomerangplatform.model.Workflow;

public abstract interface WorkflowService {

	Response createWorkflow(Workflow workflow);

	Response terminateWorkflow(Workflow workflow);
}
