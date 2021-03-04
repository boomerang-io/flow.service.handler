package net.boomerangplatform.kube.service;

import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import io.kubernetes.client.ApiException;
import io.kubernetes.client.models.V1ConfigMap;
import io.kubernetes.client.models.V1Job;
import io.kubernetes.client.models.V1PersistentVolumeClaim;
import io.kubernetes.client.models.V1PersistentVolumeClaimStatus;
import io.kubernetes.client.models.V1Status;
import net.boomerangplatform.model.TaskConfiguration;
import net.boomerangplatform.model.TaskDeletionEnum;

public interface KubeService {
  V1Job createJob(
      boolean createLifecycle,
      String workspaceId,
      String workflowName,
      String workflowId,
      String workflowActivityId,
      String taskActivityId,
      String taskName,
      String taskId,
      Map<String, String> customLabels,
      List<String> arguments,
      Map<String, String> properties,
      String image,
      String command,
      TaskConfiguration configuration);

  V1ConfigMap createTaskConfigMap(
      String workflowName,
      String workflowId,
      String workflowActivityId,
      String taskName,
      String taskId,
      String taskActivityId,
      Map<String, String> customLabels,
      Map<String, String> inputProps);

  Map<String, String> getTaskOutPutConfigMapData(
      String workflowId, String workflowActivityId, String taskId, String taskName);

  String getPodLog(
      String workflowId, String workflowActivityId, String taskId, String taskActivityId);

  StreamingResponseBody streamPodLog(
      HttpServletResponse response,
      String workflowId,
      String workflowActivityId,
      String taskId,
      String taskActivityId);

  void patchTaskConfigMap(
      String workflowId,
      String workflowActivityId,
      String taskId,
      String taskName,
      Map<String, String> properties);

  V1Status deleteJob(
      TaskDeletionEnum taskDeletion, String workflowId, String workflowActivityId, String taskId, String taskActivityId);

  V1PersistentVolumeClaim createWorkspacePVC(String workspaceName, String workspaceId, Map<String, String> customLabels,
      String size, String className, String accessMode) throws ApiException;

  V1PersistentVolumeClaim createWorkflowPVC(String workflowName, String workflowId,
      String activityId, Map<String, String> customLabels, String size, String className, String accessMode) throws ApiException;

  V1PersistentVolumeClaimStatus watchWorkspacePVC(String workspaceId);

  V1PersistentVolumeClaimStatus watchWorkflowPVC(String workflowId, String workflowActivityId);

  boolean checkWorkflowPVCExists(String workflowId, String workflowActivityId, String taskId,
      String taskActivityId, boolean failIfNotBound);

  boolean checkWorkspacePVCExists(String workspaceId, boolean failIfNotBound);

  V1Status deleteWorkspacePVC(String workspaceId);

  V1Status deleteWorkflowPVC(String workflowId, String workflowActivityId);

  V1ConfigMap watchTaskConfigMap(String workflowId, String workflowActivityId, String taskId,
      String taskActivityId);

  V1ConfigMap watchWorkflowConfigMap(String workflowId, String workflowActivityId);

  V1Status deleteTaskConfigMap(String workflowId, String workflowActivityId, String taskId,
      String taskActivityId);

  V1Status deleteWorkflowConfigMap(String workflowId, String workflowActivityId);

  V1ConfigMap createWorkflowConfigMap(String workflowName, String workflowId, String activityId,
      Map<String, String> customLabels, Map<String, String> inputProps);
}
