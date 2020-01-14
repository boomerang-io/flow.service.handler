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

public interface AbstractKubeService {

  V1PersistentVolumeClaim createPVC(
      String workflowName, String workflowId, String workflowActivityId, String pvcSize)
      throws ApiException;

  V1Status deletePVC(String workflowId, String workflowActivityId);

  V1PersistentVolumeClaimStatus watchPVC(String workflowId, String workflowActivityId);

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
      Map<String, String> taskProperties);

  V1Job createJob(
	  boolean createLifecycle,
      String workflowName,
      String workflowId,
      String workflowActivityId,
      String taskActivityId,
      String taskName,
      String taskId,
      List<String> arguments,
      Map<String, String> taskProperties,
      String image,
      String command);

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

  String getPodLog(String workflowId, String workflowActivityId, String taskId,  String taskActivityId);

  StreamingResponseBody streamPodLog(
      HttpServletResponse response, String workflowId, String workflowActivityId, String taskId, String taskActivityId);

  void patchTaskConfigMap(
      String workflowId,
      String workflowActivityId,
      String taskId,
      String taskName,
      Map<String, String> properties);

V1Status deleteJob(String workflowId,String workflowActivityId,String taskId);

}
