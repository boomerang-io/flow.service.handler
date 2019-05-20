package net.boomerangplatform.kube.service;

import java.io.IOException;
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

	V1Job watchJob(String workflowId, String workflowActivityId, String taskId) throws Exception;
	V1PersistentVolumeClaim createPVC(String workflowName, String workflowId, String workflowActivityId, String pvcSize) throws ApiException, IOException;
	V1Status deletePVC(String workflowId, String workflowActivityId);
	V1PersistentVolumeClaimStatus watchPVC(String workflowId, String workflowActivityId) throws ApiException, IOException;
	V1ConfigMap createWorkflowConfigMap(String workflowName, String workflowId, String workflowActivityId, Map<String, String> data) throws ApiException, IOException;
	V1Job createJob(String workflowName, String workflowId, String workflowActivityId, String taskName, String taskId,
			List<String> arguments, Map<String, String> taskInputProperties);
	V1ConfigMap watchConfigMap(String workflowId, String workflowActivityId, String taskId)
			throws ApiException, IOException;
	V1ConfigMap createTaskConfigMap(String workflowName, String workflowId, String workflowActivityId, String taskName,
			String taskId, Map<String, String> inputProps) throws ApiException, IOException;
	V1Status deleteConfigMap(String workflowId, String workflowActivityId, String taskId);
	Map<String, String> getTaskOutPutConfigMapData(String workflowId, String workflowActivityId, String taskId,
			String taskName);
	String getPodLog(String workflowId, String workflowActivityId, String taskId) throws ApiException, IOException;
	StreamingResponseBody streamPodLog(HttpServletResponse response, String workflowId, String workflowActivityId,
			String taskId) throws ApiException, IOException;
	void patchTaskConfigMap(String workflowId, String workflowActivityId, String taskId, String taskName,
			Map<String, String> properties);
}