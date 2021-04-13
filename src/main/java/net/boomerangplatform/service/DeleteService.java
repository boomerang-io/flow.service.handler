package net.boomerangplatform.service;

import java.util.Map;
import net.boomerangplatform.model.TaskDeletionEnum;

public interface DeleteService {

  void deleteJob(TaskDeletionEnum taskDeletion, String workflowId, String workflowActivityId,
      String taskId, String taskActivityId, Map<String, String> customLabels);
}
