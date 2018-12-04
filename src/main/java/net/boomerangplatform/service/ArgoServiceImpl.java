package net.boomerangplatform.service;

import org.springframework.stereotype.Service;

import io.kubernetes.client.ApiException;
import io.kubernetes.client.apis.CustomObjectsApi;
import net.boomerangplatform.model.argo.Workflow;

@Service
public class ArgoServiceImpl implements ArgoService {
	
	@Override
	public Workflow getWorkflow(String name) {
		CustomObjectsApi api = new CustomObjectsApi();
		Workflow response = null;
		try {
			response = (Workflow) api.getNamespacedCustomObject("argoproj.io", "v1alpha1", "default", "workflows",
					name);
		} catch (ApiException e) {
			e.printStackTrace();
		}
		return response;
	}

	@Override
	public Object createWorkflow(Workflow workflow) {
		CustomObjectsApi api = new CustomObjectsApi();
		Object response = null;
		try {
			response = api.createNamespacedCustomObject("argoproj.io", "v1alpha1", "default", "workflows",
					workflow, null);
		} catch (ApiException e) {
			e.printStackTrace();
		}
		return response;
	} 
}
