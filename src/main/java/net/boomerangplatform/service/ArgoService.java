package net.boomerangplatform.service;

import net.boomerangplatform.model.glen.Workflow;

public interface ArgoService {

	Workflow getWorkflow(String name);
	Object createWorkflow(Workflow workflow);
}