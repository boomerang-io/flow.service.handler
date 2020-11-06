package net.boomerangplatform.service;

import net.boomerangplatform.model.TaskDeletionEnum;

public interface DeleteService {

  void deleteJob(TaskDeletionEnum taskDeletion, String workflowId, String workflowActivityId,
      String taskId);

}
