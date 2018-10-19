package net.boomerangplatform.service;

import io.kubernetes.client.models.V1NamespaceList;

public interface KubeService {

	V1NamespaceList getAllNamespaces();
}