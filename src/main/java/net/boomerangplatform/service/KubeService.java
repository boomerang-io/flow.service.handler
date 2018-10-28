package net.boomerangplatform.service;

import io.kubernetes.client.models.V1Job;
import io.kubernetes.client.models.V1JobList;
import io.kubernetes.client.models.V1NamespaceList;
import net.boomerangplatform.model.Workflow;

public interface KubeService {

	V1NamespaceList getAllNamespaces();
	Workflow getWorkflow(String name);
	Object createWorkflow(Workflow workflow);
	V1JobList getAllJobs();
	V1Job createJob();
}