package net.boomerangplatform.service;

import net.boomerangplatform.model.Task;
import net.boomerangplatform.model.Workflow;

public interface ControllerService {

	String executeTask(Task task);
	String createWorkflow(Workflow workflow);
	String terminateWorkflow(Workflow workflow);
}