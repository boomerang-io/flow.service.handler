package net.boomerangplatform.kube.service;

import io.kubernetes.client.models.V1JobList;
import io.kubernetes.client.models.V1NamespaceList;

public interface KubeService {

  V1NamespaceList getAllNamespaces();

  V1JobList getAllJobs();

  void watchNamespace();
}
