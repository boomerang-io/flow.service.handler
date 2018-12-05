package net.boomerangplatform.service;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import io.kubernetes.client.ApiException;
import io.kubernetes.client.models.V1ConfigMap;
import io.kubernetes.client.models.V1Job;
import io.kubernetes.client.models.V1JobList;
import io.kubernetes.client.models.V1NamespaceList;
import io.kubernetes.client.models.V1PersistentVolumeClaim;
import io.kubernetes.client.models.V1PersistentVolumeClaimStatus;
import io.kubernetes.client.models.V1Status;

public interface KubeService {

	V1NamespaceList getAllNamespaces();
	V1JobList getAllJobs();
	void watchNamespace() throws ApiException, IOException;
	String watchJob(String workflowId, String workflowActivityId, String taskId) throws ApiException, IOException;
	V1Job createJob(String workflowName, String workflowId, String workflowActivityId, String taskId, List<String> arguments, Map<String, String> inputProperties);
	V1PersistentVolumeClaim createPVC(String workflowName, String workflowId, String workflowActivityId, String pvcSize) throws ApiException, IOException;
	V1Status deletePVC(String workflowId, String workflowActivityId);
	V1PersistentVolumeClaimStatus watchPVC(String workflowId, String workflowActivityId) throws ApiException, IOException;
	V1ConfigMap createConfigMap(String workflowName, String workflowId, String workflowActivityId, Map<String, String> data) throws ApiException, IOException;
	V1ConfigMap watchConfigMap(String workflowId, String workflowActivityId) throws ApiException, IOException;
	V1Status deleteConfigMap(String workflowId, String workflowActivityId);
}