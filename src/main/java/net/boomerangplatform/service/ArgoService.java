package net.boomerangplatform.service;

import net.boomerangplatform.model.argo.Workflow;

public interface ArgoService {

	Workflow getWorkflow(String name);
	Object createWorkflow(Workflow workflow);
}