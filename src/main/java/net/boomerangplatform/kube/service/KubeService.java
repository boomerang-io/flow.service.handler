package net.boomerangplatform.kube.service;

import java.util.Map;
import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.PersistentVolumeClaim;
import io.fabric8.kubernetes.client.KubernetesClientException;

public interface KubeService {

  boolean checkWorkspacePVCExists(String workspaceId, boolean failIfNotBound);

  boolean checkWorkflowPVCExists(String workflowId, String workflowActivityId, String taskId,
      String taskActivityId, boolean failIfNotBound);

  PersistentVolumeClaim createWorkspacePVC(String workspaceName, String workspaceId,
      Map<String, String> customLabels, String size, String className, String accessMode,
      long waitSeconds) throws KubernetesClientException, InterruptedException;

  PersistentVolumeClaim createWorkflowPVC(String workflowName, String workflowId,
      String workflowActivityId, Map<String, String> customLabels, String size, String className,
      String accessMode, long waitSeconds) throws KubernetesClientException, InterruptedException;

  void deleteWorkspacePVC(String workspaceId);

  void deleteWorkflowPVC(String workflowId, String workflowActivityId);

  ConfigMap createTaskConfigMap(String workflowName, String workflowId, String workflowActivityId,
      String taskName, String taskId, String taskActivityId, Map<String, String> customLabels,
      Map<String, String> inputProps);

  void deleteWorkflowConfigMap(String workflowId, String workflowActivityId);

  void deleteTaskConfigMap(String workflowId, String workflowActivityId, String taskId,
      String taskActivityId, Map<String, String> customLabels);

}
