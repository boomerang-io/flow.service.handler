package net.boomerangplatform.service;

import java.util.Map;

public interface DeleteService {

  void deleteJob(String workflowId, String workflowActivityId, String taskId, String taskActivityId,
      Map<String, String> customLabels);
}
