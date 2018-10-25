package net.boomerangplatform.service;

import org.springframework.stereotype.Service;

import io.kubernetes.client.ApiException;
import io.kubernetes.client.apis.CoreV1Api;
import io.kubernetes.client.apis.CustomObjectsApi;
import io.kubernetes.client.models.V1NamespaceList;
import net.boomerangplatform.model.Workflow;

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
    
    @Override
    public Workflow getWorkflow(String name) {
    	CustomObjectsApi api = new CustomObjectsApi();
    	Workflow response = null;
    	try {
    		response = (Workflow) api.getNamespacedCustomObject("argoproj.io", "v1alpha1", "default", "workflows", name);
    	}
    	catch (ApiException e) {
    		e.printStackTrace();
    	}    	
    	return response;
    }
    
    @Override
    public Object createWorkflow(Workflow workflow) {    	
    	CustomObjectsApi api = new CustomObjectsApi();
    	Object response = null;
    	try {
    		response = (Object) api.createNamespacedCustomObject("argoproj.io", "v1alpha1", "default", "workflows", workflow, null);
    	}
    	catch (ApiException e) {
    		e.printStackTrace();
    	}
    	return response;
    }
}
