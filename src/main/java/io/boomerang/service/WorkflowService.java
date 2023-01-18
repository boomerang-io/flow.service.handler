package io.boomerang.service;

import io.boomerang.model.Response;
import io.boomerang.model.WorkflowRequest;

public abstract interface WorkflowService {

	Response execute(WorkflowRequest workflow);

	Response terminate(WorkflowRequest workflow);
}
