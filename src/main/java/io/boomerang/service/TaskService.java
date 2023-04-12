package io.boomerang.service;

import io.boomerang.model.TaskRequest;
import io.boomerang.model.TaskResponse;

public abstract interface TaskService {

	TaskResponse execute(TaskRequest task);

    TaskResponse terminate(TaskRequest task);
}
