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

  V1ConfigMap createWorkflowConfigMap(
      String workflowName, String workflowId, String workflowActivityId, Map<String, String> data);

  V1Job createJob(
      boolean createLifecycle,
      String workflowName,
      String workflowId,
      String workflowActivityId,
      String taskActivityId,
      String taskName,
      String taskId,
      List<String> arguments,
      Map<String, String> properties,
      String image,
      String command,
      TaskConfiguration configuration);

  V1ConfigMap watchConfigMap(String workflowId, String workflowActivityId, String taskId);

  V1ConfigMap createTaskConfigMap(
      String workflowName,
      String workflowId,
      String workflowActivityId,
      String taskName,
      String taskId,
      Map<String, String> inputProps);

  V1Status deleteConfigMap(String workflowId, String workflowActivityId, String taskId);

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
      TaskDeletionEnum taskDeletion, String workflowId, String workflowActivityId, String taskId);

  V1PersistentVolumeClaim createWorkspacePVC(String workspaceName, String workspaceId,
      String pvcSize) throws ApiException;

  V1PersistentVolumeClaim createWorkflowPVC(String workflowName, String workflowId,
      String workflowActivityId, String pvcSize) throws ApiException;

  V1PersistentVolumeClaimStatus watchWorkspacePVC(String workspaceId);

  V1PersistentVolumeClaimStatus watchWorkflowPVC(String workflowId, String workflowActivityId);

  boolean checkWorkflowPVCExists(String workflowId, String workflowActivityId, String taskId,
      boolean failIfNotBound);

  boolean checkWorkspacePVCExists(String workspaceId, boolean failIfNotBound);

  V1Status deleteWorkspacePVC(String workspaceId);

  V1Status deleteWorkflowPVC(String workflowId, String workflowActivityId);
}
