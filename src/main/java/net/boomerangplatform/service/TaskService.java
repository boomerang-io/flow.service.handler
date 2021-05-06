package net.boomerangplatform.service;

import net.boomerangplatform.model.Task;
import net.boomerangplatform.model.TaskResponse;

public abstract interface TaskService {

	TaskResponse executeTask(Task task);

    TaskResponse terminateTask(Task task);
}
