package net.boomerangplatform.kube.service;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.delete;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.okForContentType;
import static com.github.tomakehurst.wiremock.client.WireMock.patch;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import io.kubernetes.client.ApiClient;
import io.kubernetes.client.ApiException;
import io.kubernetes.client.Configuration;
import io.kubernetes.client.models.V1ConfigMap;
import io.kubernetes.client.models.V1Job;
import io.kubernetes.client.models.V1PersistentVolumeClaim;
import io.kubernetes.client.models.V1Status;
import io.kubernetes.client.util.ClientBuilder;
import net.boomerangplatform.kube.exception.KubeRuntimeException;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest
@ActiveProfiles("cicd")
public class CICDKubeServiceTest {

  @Autowired
  private CICDKubeServiceImpl cicdKubeService;

  private ApiClient client;

  private static final int PORT = 8089;
  @Rule
  public WireMockRule wireMockRule = new WireMockRule(PORT);

  @Before
  public void setup() throws IOException {
    client = new ClientBuilder().setBasePath("http://localhost:" + PORT).build();
    Configuration.setDefaultApiClient(client);
    cicdKubeService.setApiClient(client);
  }

  @Test
  public void testCreateJob() {
    List<String> arguments = new ArrayList<>();
    Map<String, String> taskInputProperties = new HashMap<>();
    taskInputProperties.put("name1", "value1");

    stubFor(post(urlPathMatching("/apis/batch/v1/namespaces/default/jobs")).willReturn(
        okForContentType(MediaType.APPLICATION_JSON_VALUE, "{\"apiVersion\": \"1.0\"}")));

    stubFor(get(urlPathMatching("/api/v1/namespaces/default/configmaps"))
        .willReturn(okForContentType(MediaType.APPLICATION_JSON_VALUE,
            "{\"apiVersion\": \"1.0\", \"metadata\": {\"name\": \"configMapMetadataName\"}, \"items\": [{\"metadata\": {\"name\": \"metadataName\"}}]}")));

    stubFor(get(urlPathMatching("/api/v1/namespaces/default/persistentvolumeclaims"))
        .willReturn(okForContentType(MediaType.APPLICATION_JSON_VALUE,
            "{\"apiVersion\": \"1.0\", \"items\": [{\"metadata\": {\"name\": \"metadataName\"}, \"status\": {\"phase\" : \"Bound\"}}]}")));

    V1Job job = cicdKubeService.createJob("workflowName", "workflowId", "workflowActivityId",
        "taskName", "taskId", arguments, taskInputProperties);

    assertNotNull(job);
    assertEquals("1.0", job.getApiVersion());
  }

  @Test
  public void testCreateJobWithEmptyResponses() {
    stubFor(post(urlPathMatching("/apis/batch/v1/namespaces/default/jobs")).willReturn(
        okForContentType(MediaType.APPLICATION_JSON_VALUE, "{\"apiVersion\": \"1.0\"}")));

    stubFor(get(urlPathMatching("/api/v1/namespaces/default/configmaps"))
        .willReturn(okForContentType(MediaType.APPLICATION_JSON_VALUE,
            "{\"apiVersion\": \"1.0\", \"metadata\": {\"name\": \"\"}, \"items\": []}")));

    stubFor(get(urlPathMatching("/api/v1/namespaces/default/persistentvolumeclaims"))
        .willReturn(okForContentType(MediaType.APPLICATION_JSON_VALUE,
            "{\"apiVersion\": \"1.0\", \"items\": []}")));

    V1Job job = cicdKubeService.createJob("workflowName", "workflowId", "workflowActivityId",
        "taskName", "taskId", null, null);

    assertNotNull(job);
    assertEquals("1.0", job.getApiVersion());
  }

  @Test
  public void testCreateJobWithException() {
    List<String> arguments = Arrays.asList("argument");
    Map<String, String> taskInputProperties = new HashMap<>();
    taskInputProperties.put("name1", "value1");

    stubFor(post(urlPathMatching("/apis/batch/v1/namespaces/default/jobs")).willReturn(aResponse()
        .withStatus(404).withHeader("Content-Type", "text/plain").withBody("Not Found")));

    stubFor(get(urlPathMatching("/api/v1/namespaces/default/configmaps")).willReturn(aResponse()
        .withStatus(404).withHeader("Content-Type", "text/plain").withBody("Not Found")));

    stubFor(get(urlPathMatching("/api/v1/namespaces/default/persistentvolumeclaims"))
        .willReturn(aResponse().withStatus(404).withHeader("Content-Type", "text/plain")
            .withBody("Not Found")));

    V1Job job = cicdKubeService.createJob("workflowName", "workflowId", "workflowActivityId",
        "taskName", "taskId", arguments, taskInputProperties);

    assertNotNull(job);
    assertNull(job.getApiVersion());
  }

  @Test
  public void testCreateWorkflowConfigMap() {
    stubFor(post(urlPathMatching("/api/v1/namespaces/default/configmaps"))
        .willReturn(okForContentType(MediaType.APPLICATION_JSON_VALUE,
            "{\"apiVersion\": \"1.0\", \"metadata\": {\"name\": \"configMapMetadataName\"}, \"items\": [{\"metadata\": {\"name\": \"metadataName\"}}]}")));

    V1ConfigMap configMap = cicdKubeService.createWorkflowConfigMap("workflowName", "workflowId",
        "workflowActivityId", new HashMap<>());

    assertNotNull(configMap);
    assertEquals("1.0", configMap.getApiVersion());
  }

  @Test(expected = KubeRuntimeException.class)
  public void testCreateWorkflowConfigMapWithException() {
    stubFor(post(urlPathMatching("/api/v1/namespaces/default/configmaps")).willReturn(aResponse()
        .withStatus(404).withHeader("Content-Type", "text/plain").withBody("Not Found")));

    cicdKubeService.createWorkflowConfigMap("workflowName", "workflowId", "workflowActivityId",
        new HashMap<>());
  }

  @Test
  public void testCreateTaskConfigMap() {
    stubFor(post(urlPathMatching("/api/v1/namespaces/default/configmaps"))
        .willReturn(okForContentType(MediaType.APPLICATION_JSON_VALUE,
            "{\"apiVersion\": \"1.0\", \"metadata\": {\"name\": \"configMapMetadataName\"}, \"items\": [{\"metadata\": {\"name\": \"metadataName\"}}]}")));

    V1ConfigMap configMap = cicdKubeService.createTaskConfigMap("workflowName", "workflowId",
        "workflowActivityId", "taskName", "taskId", new HashMap<>());

    assertNotNull(configMap);
    assertEquals("1.0", configMap.getApiVersion());
  }

  @Test
  public void testGetPodLog() {
    stubFor(get(urlPathMatching("/api/v1/namespaces/default/pods")).willReturn(okForContentType(
        MediaType.APPLICATION_JSON_VALUE,
        "{\"apiVersion\": \"1.0\", \"metadata\": {\"name\": \"configMapMetadataName\"}, \"items\": [{\"metadata\": {\"name\": \"name\", \"namespace\": \"namespace\"}, \"spec\": {\"containers\": [{\"name\": \"containerName\"}]}}]}")));

    stubFor(get(urlPathMatching("/api/v1/namespaces/namespace/pods/name/log"))
        .willReturn(okForContentType(MediaType.APPLICATION_JSON_VALUE, "{}")));

    String log = cicdKubeService.getPodLog("workflowId", "workflowActivityId", "taskId");

    assertNotNull(log);
  }

  @Test(expected = KubeRuntimeException.class)
  public void testGetPodLogWithException() {
    stubFor(get(urlPathMatching("/api/v1/namespaces/default/pods")).willReturn(aResponse()
        .withStatus(404).withHeader("Content-Type", "text/plain").withBody("Not Found")));

    cicdKubeService.getPodLog("workflowId", "workflowActivityId", "taskId");
  }

  @Test
  public void testCreatePVC() throws ApiException {
    stubFor(post(urlPathMatching("/api/v1/namespaces/default/persistentvolumeclaims"))
        .willReturn(okForContentType(MediaType.APPLICATION_JSON_VALUE,
            "{\"apiVersion\": \"1.0\", \"items\": [{\"metadata\": {\"name\": \"metadataName\"}, \"status\": {\"phase\" : \"Bound\"}}]}")));

    V1PersistentVolumeClaim persistentVolumeClaim =
        cicdKubeService.createPVC("workflowName", "workflowId", "workflowActivityId", null);

    assertNotNull(persistentVolumeClaim);
  }

  @Test
  public void testDeletePVC() throws ApiException {
    stubFor(delete(urlPathMatching("/api/v1/namespaces/default/persistentvolumeclaims/"))
        .willReturn(okForContentType(MediaType.APPLICATION_JSON_VALUE,
            "{\"apiVersion\": \"1.0\", \"items\": [{\"metadata\": {\"name\": \"metadataName\"}, \"status\": {\"phase\" : \"Bound\"}}]}")));

    stubFor(get(urlPathMatching("/api/v1/namespaces/default/persistentvolumeclaims"))
        .willReturn(okForContentType(MediaType.APPLICATION_JSON_VALUE,
            "{\"apiVersion\": \"1.0\", \"items\": [{\"metadata\": {\"name\": \"metadataName\"}, \"status\": {\"phase\" : \"Bound\"}}]}")));

    stubFor(
        delete(urlPathMatching("/api/v1/namespaces/default/persistentvolumeclaims/metadataName"))
            .willReturn(
                okForContentType(MediaType.APPLICATION_JSON_VALUE, "{\"apiVersion\": \"1.0\"}")));


    V1Status status = cicdKubeService.deletePVC("workflowId", "workflowActivityId");

    assertNotNull(status);
    assertEquals("1.0", status.getApiVersion());
  }

  @Test
  public void testDeletePVCWithJsonSyntaxException() throws ApiException {
    stubFor(delete(urlPathMatching("/api/v1/namespaces/default/persistentvolumeclaims/"))
        .willReturn(okForContentType(MediaType.APPLICATION_JSON_VALUE, "{\"apiVersion}")));
    stubFor(get(urlPathMatching("/api/v1/namespaces/default/persistentvolumeclaims"))
        .willReturn(okForContentType(MediaType.APPLICATION_JSON_VALUE,
            "{\"apiVersion\": \"1.0\", \"items\": [{\"metadata\": {\"name\": \"metadataName\"}, \"status\": {\"phase\" : \"Bound\"}}]}")));
    stubFor(
        delete(urlPathMatching("/api/v1/namespaces/default/persistentvolumeclaims/metadataName"))
            .willReturn(
                okForContentType(MediaType.APPLICATION_JSON_VALUE, "{\"apiVersion\": {}}")));

    V1Status status = cicdKubeService.deletePVC("workflowId", "workflowActivityId");

    assertNotNull(status);
    assertNull(status.getApiVersion());
  }

  @Test
  public void testDeletePVCWithApiException() throws ApiException {
    stubFor(delete(urlPathMatching("/api/v1/namespaces/default/persistentvolumeclaims/"))
        .willReturn(okForContentType(MediaType.APPLICATION_JSON_VALUE, "{}")));
    stubFor(get(urlPathMatching("/api/v1/namespaces/default/persistentvolumeclaims"))
        .willReturn(okForContentType(MediaType.APPLICATION_JSON_VALUE,
            "{\"apiVersion\": \"1.0\", \"items\": [{\"metadata\": {\"name\": \"metadataName\"}, \"status\": {\"phase\" : \"Bound\"}}]}")));
    stubFor(
        delete(urlPathMatching("/api/v1/namespaces/default/persistentvolumeclaims/metadataName"))
            .willReturn(aResponse().withStatus(404).withHeader("Content-Type", "text/plain")
                .withBody("Not Found")));

    V1Status status = cicdKubeService.deletePVC("workflowId", "workflowActivityId");

    assertNotNull(status);
    assertNull(status.getApiVersion());
  }

  @Test
  public void testPatchTaskConfigMap() {
    stubFor(get(urlPathMatching("/api/v1/namespaces/default/configmaps"))
        .willReturn(okForContentType(MediaType.APPLICATION_JSON_VALUE,
            "{\"apiVersion\": \"1.0\", \"metadata\": {\"name\": \"configMapMetadataName\"}, \"items\": [{\"metadata\": {\"name\": \"metadataName\"}, \"data\": {\"key\":\"value\"}}]}")));

    stubFor(patch(urlPathMatching("/api/v1/namespaces/default/configmaps/metadataName"))
        .willReturn(okForContentType(MediaType.APPLICATION_JSON_VALUE,
            "{\"apiVersion\": \"1.0\", \"metadata\": {\"name\": \"configMapMetadataName\"}, \"items\": [{\"metadata\": {\"name\": \"metadataName\"}, \"data\": {\"key\":\"value\"}}]}")));

    cicdKubeService.patchTaskConfigMap("workflowId", "workflowActivityId", "taskId", "taskName",
        new HashMap<>());
  }

  @Test
  public void testPatchTaskConfigMapWithException() {
    stubFor(get(urlPathMatching("/api/v1/namespaces/default/configmaps"))
        .willReturn(okForContentType(MediaType.APPLICATION_JSON_VALUE,
            "{\"apiVersion\": \"1.0\", \"metadata\": {\"name\": \"configMapMetadataName\"}, \"items\": [{\"metadata\": {\"name\": \"metadataName\"}, \"data\": {\"taskName.output.properties\":\"value\"}}]}")));

    stubFor(patch(urlPathMatching("/api/v1/namespaces/default/configmaps/metadataName"))
        .willReturn(aResponse().withStatus(404).withHeader("Content-Type", "text/plain")
            .withBody("Not Found")));

    cicdKubeService.patchTaskConfigMap("workflowId", "workflowActivityId", "taskId", "taskName",
        new HashMap<>());
  }

  @Test
  public void testGetTaskOutPutConfigMapData() {
    stubFor(get(urlPathMatching("/api/v1/namespaces/default/configmaps"))
        .willReturn(okForContentType(MediaType.APPLICATION_JSON_VALUE,
            "{\"apiVersion\": \"1.0\", \"metadata\": {\"name\": \"configMapMetadataName\"}, \"items\": [{\"metadata\": {\"name\": \"metadataName\"}, \"data\": {\"key\":\"value\"}}]}")));

    Map<String, String> response = cicdKubeService.getTaskOutPutConfigMapData("workflowId",
        "workflowActivityId", "taskId", "taskName");
    assertNotNull(response);
  }


  @Test
  public void testDeleteConfigMap() throws ApiException {
    stubFor(delete(urlPathMatching("/api/v1/namespaces/default/configmaps/"))
        .willReturn(okForContentType(MediaType.APPLICATION_JSON_VALUE,
            "{\"apiVersion\": \"1.0\", \"items\": [{\"metadata\": {\"name\": \"metadataName\"}, \"status\": {\"phase\" : \"Bound\"}}]}")));

    stubFor(get(urlPathMatching("/api/v1/namespaces/default/configmaps"))
        .willReturn(okForContentType(MediaType.APPLICATION_JSON_VALUE,
            "{\"apiVersion\": \"1.0\", \"items\": [{\"metadata\": {\"name\": \"metadataName\"}, \"status\": {\"phase\" : \"Bound\"}}]}")));

    stubFor(
        delete(urlPathMatching("/api/v1/namespaces/default/configmaps/metadataName")).willReturn(
            okForContentType(MediaType.APPLICATION_JSON_VALUE, "{\"apiVersion\": \"1.0\"}")));

    V1Status status = cicdKubeService.deleteConfigMap("workflowId", "workflowActivityId", "taskId");

    assertNotNull(status);
    assertEquals("1.0", status.getApiVersion());
  }

  @Test
  public void testDeleteConfigMapWithJsonSyntaxException() throws ApiException {
    stubFor(delete(urlPathMatching("/api/v1/namespaces/default/configmaps/"))
        .willReturn(okForContentType(MediaType.APPLICATION_JSON_VALUE, "{\"apiVersion}")));
    stubFor(get(urlPathMatching("/api/v1/namespaces/default/configmaps"))
        .willReturn(okForContentType(MediaType.APPLICATION_JSON_VALUE,
            "{\"apiVersion\": \"1.0\", \"items\": [{\"metadata\": {\"name\": \"metadataName\"}, \"status\": {\"phase\" : \"Bound\"}}]}")));
    stubFor(delete(urlPathMatching("/api/v1/namespaces/default/configmaps/metadataName"))
        .willReturn(okForContentType(MediaType.APPLICATION_JSON_VALUE, "{\"apiVersion\": {}}")));

    V1Status status = cicdKubeService.deleteConfigMap("workflowId", "workflowActivityId", "taskId");

    assertNotNull(status);
    assertNull(status.getApiVersion());
  }

  @Test
  public void testDeleteConfigMapWithApiException() throws ApiException {
    stubFor(delete(urlPathMatching("/api/v1/namespaces/default/configmaps/"))
        .willReturn(okForContentType(MediaType.APPLICATION_JSON_VALUE, "{}")));
    stubFor(get(urlPathMatching("/api/v1/namespaces/default/configmaps"))
        .willReturn(okForContentType(MediaType.APPLICATION_JSON_VALUE,
            "{\"apiVersion\": \"1.0\", \"items\": [{\"metadata\": {\"name\": \"metadataName\"}, \"status\": {\"phase\" : \"Bound\"}}]}")));
    stubFor(delete(urlPathMatching("/api/v1/namespaces/default/configmaps/metadataName"))
        .willReturn(aResponse().withStatus(404).withHeader("Content-Type", "text/plain")
            .withBody("Not Found")));

    V1Status status = cicdKubeService.deleteConfigMap("workflowId", "workflowActivityId", "taskId");

    assertNotNull(status);
    assertNull(status.getApiVersion());
  }
}
