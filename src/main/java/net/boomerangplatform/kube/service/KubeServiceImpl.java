package net.boomerangplatform.kube.service;

import java.io.IOException;
import java.util.concurrent.TimeUnit;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import com.google.gson.reflect.TypeToken;
import io.kubernetes.client.ApiClient;
import io.kubernetes.client.ApiException;
import io.kubernetes.client.Configuration;
import io.kubernetes.client.apis.BatchV1Api;
import io.kubernetes.client.apis.CoreV1Api;
import io.kubernetes.client.models.V1JobList;
import io.kubernetes.client.models.V1Namespace;
import io.kubernetes.client.models.V1NamespaceList;
import io.kubernetes.client.util.Config;
import io.kubernetes.client.util.Watch;
import net.boomerangplatform.kube.exception.KubeRuntimeException;

@Service
public class KubeServiceImpl implements KubeService {

  private static final int TIMEOUT = 120;

  private static final int LIMIT = 25;

  private static final int TIMEOUT_ONE_MINUTE = 60;

  private static final Logger LOGGER = LogManager.getLogger(KubeServiceImpl.class);

  @Value("${kube.image}")
  private String kubeImage;

  @Value("${kube.namespace}")
  protected String kubeNamespace;

  @Value("${kube.api.pretty}")
  private String kubeApiPretty;

  @Value("${kube.api.includeunitialized}")
  protected Boolean kubeApiIncludeuninitialized;

  private ApiClient apiClient; // NOSONAR

  @Override
  public V1NamespaceList getAllNamespaces() {
    V1NamespaceList list = new V1NamespaceList();
    try {
      list = getCoreApi().listNamespace(null, null, null, null, null, null, null, null, null);
    } catch (ApiException e) {
      LOGGER.error("Exception: ", e);
    }
    return list;
  }

  @Override
  public V1JobList getAllJobs() {
    V1JobList list = new V1JobList();

    try {
      list = getBatchApi().listNamespacedJob(kubeNamespace, kubeApiIncludeuninitialized,
          kubeApiPretty, "", "", "", LIMIT, "", TIMEOUT_ONE_MINUTE, false);

    } catch (ApiException e) {
      LOGGER.error("Exception: ", e);
    }
    return list;
  }

  @Override
  public void watchNamespace() {
    try {
      Watch<V1Namespace> watch = Watch.createWatch(
          getDefaultClient(), getCoreApi().listNamespaceCall(null, null, null, null, null, TIMEOUT,
              null, null, Boolean.TRUE, null, null),
          new TypeToken<Watch.Response<V1Namespace>>() {}.getType());

      logNamespace(watch);

    } catch (ApiException | IOException e) {
      throw new KubeRuntimeException("watchNamespace Exception: ", e);
    }
  }

  private void logNamespace(Watch<V1Namespace> watch) throws IOException {
    try {
      for (Watch.Response<V1Namespace> item : watch) {
        String type = item.type == null ? "" : item.type;
        String name = item.object == null ? "" : item.object.getMetadata().getName();
        LOGGER.info(String.format("%s : %s%n", type, name));
      }
    } finally {
      watch.close();
    }
  }

  private ApiClient getDefaultClient() throws IOException {
    if (apiClient != null) {
      return apiClient;
    }

    ApiClient client = Config.defaultClient();
    client.getHttpClient().setReadTimeout(TIMEOUT_ONE_MINUTE, TimeUnit.SECONDS);
    Configuration.setDefaultApiClient(client);
    return client;
  }

  private CoreV1Api getCoreApi() {
    return apiClient == null ? new CoreV1Api() : new CoreV1Api(apiClient);
  }

  private BatchV1Api getBatchApi() {
    return apiClient == null ? new BatchV1Api() : new BatchV1Api(apiClient);
  }

  void setApiClient(ApiClient apiClient) {
    this.apiClient = apiClient;
  }
}
