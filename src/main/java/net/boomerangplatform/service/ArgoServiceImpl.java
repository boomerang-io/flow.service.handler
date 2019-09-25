package net.boomerangplatform.service;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import io.kubernetes.client.ApiClient;
import io.kubernetes.client.ApiException;
import io.kubernetes.client.JSON;
import io.kubernetes.client.apis.CustomObjectsApi;
import net.boomerangplatform.model.argo.Workflow;

@Service
public class ArgoServiceImpl implements ArgoService {

  private static final Logger LOGGER = LogManager.getLogger(ArgoServiceImpl.class);

  private ApiClient apiClient; // NOSONAR

  @Override
  public Workflow getWorkflow(String name) {
    Workflow response = null;
    try {
      Object resp = getApi().getNamespacedCustomObject("argoproj.io", "v1alpha1", "default",
          "workflows", name);

      response = convertObjectToWorkflow(resp);
    } catch (ApiException e) {
      LOGGER.error("Exception: ", e);
    }
    return response;
  }

  private Workflow convertObjectToWorkflow(Object obj) {
    if (obj == null) {
      return null;
    }
    Gson gson = new JSON().getGson();
    JsonElement jsonElement = gson.toJsonTree(obj);
    return gson.fromJson(jsonElement, Workflow.class);
  }



  @Override
  public Object createWorkflow(Workflow workflow) {
    Object response = null;
    try {
      response = getApi().createNamespacedCustomObject("argoproj.io", "v1alpha1", "default",
          "workflows", workflow, null);
    } catch (ApiException e) {
      LOGGER.error("Exception: ", e);
    }
    return response;
  }

  private CustomObjectsApi getApi() {
    return apiClient == null ? new CustomObjectsApi() : new CustomObjectsApi(apiClient);
  }

  void setApiClient(ApiClient apiClient) {
    this.apiClient = apiClient;
  }
}
