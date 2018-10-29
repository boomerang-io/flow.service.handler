package net.boomerangplatform.service;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import io.kubernetes.client.ApiException;
import io.kubernetes.client.models.V1Job;
import io.kubernetes.client.models.V1JobList;
import io.kubernetes.client.models.V1NamespaceList;

public interface KubeService {

	V1NamespaceList getAllNamespaces();
	V1JobList getAllJobs();
	void watchJob() throws ApiException, IOException;
	void watchNamespace() throws ApiException, IOException;
	String watchJob(String workflowId, String taskId) throws ApiException, IOException;
	V1Job createJob(String workflowName, String workflowId, String taskId, List<String> arguments, Map<String, String> inputProperties);
}