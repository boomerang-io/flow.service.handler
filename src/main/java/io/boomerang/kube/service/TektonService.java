package io.boomerang.kube.service;

import java.text.ParseException;
import java.util.List;
import java.util.Map;
import io.boomerang.model.TaskConfiguration;
import io.boomerang.model.TaskEnvVar;
import io.boomerang.model.TaskResponseResultParameter;
import io.boomerang.model.TaskResultParameter;
import io.fabric8.tekton.pipeline.v1beta1.TaskRun;

public interface TektonService {

  TaskRun createTaskRun(String workspaceId, String workflowName, String workflowId,
      String workflowActivityId, String taskActivityId, String taskName, String taskId,
      Map<String, String> customLabels, String image, List<String> command, List<String> arguments,
      Map<String, String> parameters, List<TaskEnvVar> envVars, List<TaskResultParameter> results,
      String workingDir, TaskConfiguration configuration, String script, long waitSeconds,
      Integer timeout) throws InterruptedException, ParseException;

  List<TaskResponseResultParameter> watchTask(String workflowId, String workflowActivityId,
      String taskId, String taskActivityId, Map<String, String> customLabels, Integer timeout)
      throws InterruptedException;

  void deleteTask(String workflowId, String workflowActivityId, String taskId,
      String taskActivityId, Map<String, String> customLabels);

  void cancelTask(String workflowId, String workflowActivityId, String taskId,
      String taskActivityId, Map<String, String> customLabels);

}
