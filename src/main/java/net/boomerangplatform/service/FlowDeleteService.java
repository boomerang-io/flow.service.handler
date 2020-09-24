package net.boomerangplatform.service;

import net.boomerangplatform.model.TaskDeletion;

public interface FlowDeleteService {

  void deleteJob(TaskDeletion taskDeletion, String workflowId, String workflowActivityId,
      String taskId);

}
