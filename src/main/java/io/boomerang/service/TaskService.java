package io.boomerang.service;

import io.boomerang.model.Task;
import io.boomerang.model.TaskResponse;

public abstract interface TaskService {

	TaskResponse executeTask(Task task);

    TaskResponse terminateTask(Task task);
}
