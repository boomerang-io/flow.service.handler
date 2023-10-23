package io.boomerang.kube.service;

import java.text.ParseException;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.JsonProcessingException;

import io.boomerang.model.TaskConfiguration;
import io.boomerang.model.TaskEnvVar;
import io.boomerang.model.TaskResponseResultParameter;
import io.boomerang.model.TaskResultParameter;
import io.boomerang.model.TaskWorkspace;
import io.fabric8.tekton.pipeline.v1beta1.TaskRun;

public interface TektonService {

  List<TaskResponseResultParameter> watchTask(String workflowId, String workflowActivityId,
      String taskId, String taskActivityId, Map<String, String> customLabels, Integer timeout)
      throws InterruptedException;

  void deleteTask(String workflowId, String workflowActivityId, String taskId,
      String taskActivityId, Map<String, String> customLabels);

  void cancelTask(String workflowId, String workflowActivityId, String taskId,
      String taskActivityId, Map<String, String> customLabels);

  TaskRun createTaskRun(String workflowName, String workflowId, String workflowActivityId,
      String taskActivityId, String taskName, String taskId, Map<String, String> customLabels,
      String image, List<String> command, String script, List<String> arguments,
      Map<String, String> parameters, List<TaskEnvVar> envVars, List<TaskResultParameter> results,
      String workingDir, TaskConfiguration configuration, List<TaskWorkspace> workspaces,
      long waitSeconds, Integer timeout, String serviceAccountName, String podSecurityContextInYaml)throws InterruptedException, ParseException, JsonProcessingException;

}
