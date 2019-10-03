package net.boomerangplatform.kube.service;

import java.io.IOException;

import io.kubernetes.client.ApiException;
import io.kubernetes.client.models.V1JobList;
import io.kubernetes.client.models.V1NamespaceList;

public interface KubeService {

	V1NamespaceList getAllNamespaces();
	V1JobList getAllJobs();
  void watchNamespace() throws ApiException, IOException;
}