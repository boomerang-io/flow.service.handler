package net.boomerangplatform.service;

import org.springframework.stereotype.Service;

import io.kubernetes.client.ApiException;
import io.kubernetes.client.apis.CoreV1Api;
import io.kubernetes.client.models.V1NamespaceList;

@Service
public class KubeServiceImpl implements KubeService {

    @Override
    public V1NamespaceList getAllNamespaces() {
        V1NamespaceList list = new V1NamespaceList();         		
        try {
        	CoreV1Api api = new CoreV1Api();
			list = api.listNamespace(null, null, null, null, null, null, null, null, null);
		} catch (ApiException e) {
			e.printStackTrace();
		}        
        return list;
    }
}
