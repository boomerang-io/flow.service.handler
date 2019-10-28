package net.boomerangplatform.kube.service;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.okForContentType;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import java.io.IOException;
import java.util.ArrayList;
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
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import io.kubernetes.client.ApiClient;
import io.kubernetes.client.ApiException;
import io.kubernetes.client.Configuration;
import io.kubernetes.client.models.V1ConfigMap;
import io.kubernetes.client.models.V1Job;
import io.kubernetes.client.models.V1PersistentVolumeClaimStatus;
import io.kubernetes.client.util.ClientBuilder;
import net.boomerangplatform.Application;
import net.boomerangplatform.kube.exception.KubeRuntimeException;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest
@ContextConfiguration(classes = {Application.class, BaseKubeTest.class})
@ActiveProfiles("live")
public class FlowKubeServiceTest {

  @Autowired
  private FlowKubeServiceImpl flowKubeService;

  private ApiClient client;

  private static final int PORT = 8089;

  @Rule
  public WireMockRule wireMockRule = new WireMockRule(PORT);

  @Before
  public void setup() throws IOException {
    client = new ClientBuilder().setBasePath("http://localhost:" + PORT).build();
    Configuration.setDefaultApiClient(client);
    flowKubeService.setApiClient(client);
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
            "{\"apiVersion\": \"1.0\", \"items\": [{\"metadata\": {\"name\": \"metadataName\"}}]}")));

    V1Job job = flowKubeService.createJob("workflowName", "workflowId", "workflowActivityId","taskActivityId",
        "taskName", "taskId", arguments, taskInputProperties);

    assertNotNull(job);
    assertEquals("1.0", job.getApiVersion());
  }

  @Test
  public void testCreateJobWithException() {
    List<String> arguments = new ArrayList<>();
    Map<String, String> taskInputProperties = new HashMap<>();
    taskInputProperties.put("name1", "value1");

    stubFor(post(urlPathMatching("/apis/batch/v1/namespaces/default/jobs")).willReturn(aResponse()
        .withStatus(404).withHeader("Content-Type", "text/plain").withBody("Not Found")));

    stubFor(get(urlPathMatching("/api/v1/namespaces/default/configmaps")).willReturn(aResponse()
        .withStatus(404).withHeader("Content-Type", "text/plain").withBody("Not Found")));

    stubFor(get(urlPathMatching("/api/v1/namespaces/default/persistentvolumeclaims"))
        .willReturn(aResponse().withStatus(404).withHeader("Content-Type", "text/plain")
            .withBody("Not Found")));

    V1Job job = flowKubeService.createJob("workflowName", "workflowId", "workflowActivityId","taskActivityId",
        "taskName", "taskId", arguments, taskInputProperties);

    assertNotNull(job);
    assertNull(job.getApiVersion());
  }

  @Test
  public void testCreateWorkflowConfigMap() {
    stubFor(post(urlPathMatching("/api/v1/namespaces/default/configmaps"))
        .willReturn(okForContentType(MediaType.APPLICATION_JSON_VALUE,
            "{\"apiVersion\": \"1.0\", \"metadata\": {\"name\": \"configMapMetadataName\"}, \"items\": [{\"metadata\": {\"name\": \"metadataName\"}}]}")));

    V1ConfigMap configMap = flowKubeService.createWorkflowConfigMap("workflowName", "workflowId",
        "workflowActivityId", new HashMap<>());

    assertNotNull(configMap);
    assertEquals("1.0", configMap.getApiVersion());
  }

  @Test(expected = KubeRuntimeException.class)
  public void testCreateWorkflowConfigMapWithException() {
    stubFor(post(urlPathMatching("/api/v1/namespaces/default/configmaps")).willReturn(aResponse()
        .withStatus(404).withHeader("Content-Type", "text/plain").withBody("Not Found")));

    flowKubeService.createWorkflowConfigMap("workflowName", "workflowId", "workflowActivityId",
        new HashMap<>());
  }

  @Test
  public void testCreateTaskConfigMap() {
    stubFor(post(urlPathMatching("/api/v1/namespaces/default/configmaps"))
        .willReturn(okForContentType(MediaType.APPLICATION_JSON_VALUE,
            "{\"apiVersion\": \"1.0\", \"metadata\": {\"name\": \"configMapMetadataName\"}, \"items\": [{\"metadata\": {\"name\": \"metadataName\"}}]}")));

    V1ConfigMap configMap = flowKubeService.createTaskConfigMap("workflowName", "workflowId",
        "workflowActivityId", "taskName", "taskId", new HashMap<>());

    assertNotNull(configMap);
    assertEquals("1.0", configMap.getApiVersion());
  }

  @Test
  public void testWatchJob() {
    stubFor(get(urlPathMatching("/apis/batch/v1/namespaces/default/jobs"))
        .willReturn(okForContentType(MediaType.APPLICATION_JSON_VALUE,
            "{\"object\": {\"apiVersion\": \"1.0\", \"metadata\": {\"name\": \"configMapMetadataName\"}, \"items\": [{\"metadata\": {\"name\": \"metadataName\"}}], \"status\": { \"succeeded\" : 1}}}")));

    V1Job job = flowKubeService.watchJob("workflowId", "workflowActivityId", "taskId");

    assertNotNull(job);
  }

  @Test(expected = KubeRuntimeException.class)
  public void testWatchJobWithNoStatus() {
    stubFor(get(urlPathMatching("/apis/batch/v1/namespaces/default/jobs"))
        .willReturn(okForContentType(MediaType.APPLICATION_JSON_VALUE,
            "{\"object\": {\"apiVersion\": \"1.0\", \"metadata\": {\"name\": \"configMapMetadataName\"}, \"items\": [{\"metadata\": {\"name\": \"metadataName\"}}], \"status\": {\"failed\": 7}}}")));

    flowKubeService.watchJob("workflowId", "workflowActivityId", "taskId");
  }

  @Test(expected = KubeRuntimeException.class)
  public void testWatchJobWithException() {
    stubFor(get(urlPathMatching("/apis/batch/v1/namespaces/default/jobs")).willReturn(aResponse()
        .withStatus(404).withHeader("Content-Type", "text/plain").withBody("Not Found")));

    flowKubeService.watchJob("workflowId", "workflowActivityId", "taskId");
  }

  @Test
  public void testWatchPVC() throws ApiException {
    stubFor(get(urlPathMatching("/api/v1/namespaces/default/persistentvolumeclaims"))
        .willReturn(okForContentType(MediaType.APPLICATION_JSON_VALUE,
            "{\"type\": \"type\", \"object\": {\"apiVersion\": \"1.0\", \"metadata\": {\"name\": \"claimMetadata\"}, \"status\": {\"phase\": \"Bound\"}, \"items\": [{\"metadata\": {\"name\": \"metadataName\"}}]}}")));

    V1PersistentVolumeClaimStatus persistentVolumeClaimStatus =
        flowKubeService.watchPVC("workflowId", "workflowActivityId");

    assertNotNull(persistentVolumeClaimStatus);
  }

  @Test(expected = KubeRuntimeException.class)
  public void testWatchPVCWithException() throws ApiException {
    stubFor(get(urlPathMatching("/api/v1/namespaces/default/persistentvolumeclaims"))
        .willReturn(aResponse().withStatus(404).withHeader("Content-Type", "text/plain")
            .withBody("Not Found")));

    flowKubeService.watchPVC("workflowId", "workflowActivityId");
  }

  @Test
  public void testStreamPodLog() throws ApiException {
    stubFor(get(urlPathMatching("/api/v1/namespaces/default/pods")).willReturn(okForContentType(
        MediaType.APPLICATION_JSON_VALUE,
        "{\"type\": \"type\", \"object\": {\"apiVersion\": \"1.0\", \"metadata\": {\"name\": \"configMapMetadataName\", \"namespace\": \"namespace\", \"containers\": [{\"name\": \"containerName\"}]}, \"status\": {\"conditions\": [{}], \"containerStatuses\": [{}], \"phase\": \"Bound\"}, \"spec\": {\"containers\": [{\"name\": \"containerName\"}]}}}")));

    stubFor(get(urlPathMatching("/api/v1/namespaces/namespace/pods/configMapMetadataName/log"))
        .willReturn(okForContentType(MediaType.APPLICATION_JSON_VALUE,
            "{\"type\": \"type\", \"object\": {\"apiVersion\": \"1.0\", \"metadata\": {\"name\": \"configMapMetadataName\", \"namespace\": \"namespace\", \"containers\": [{\"name\": \"containerName\"}]}, \"status\": {\"conditions\": [{}], \"containerStatuses\": [{}], \"phase\": \"Bound\"}, \"spec\": {\"containers\": [{\"name\": \"containerName\"}]}}}")));

    StreamingResponseBody streamingResponseBody = flowKubeService
        .streamPodLog(new MockHttpServletResponse(), "workflowId", "workflowActivityId", "taskId", "taskActivityId");

    assertNotNull(streamingResponseBody);
  }

  @Test(expected = KubeRuntimeException.class)
  public void testStreamPodLogWithException() throws ApiException {
    stubFor(get(urlPathMatching("/api/v1/namespaces/default/pods")).willReturn(aResponse()
        .withStatus(404).withHeader("Content-Type", "text/plain").withBody("Not Found")));

    flowKubeService.streamPodLog(new MockHttpServletResponse(), "workflowId", "workflowActivityId",
        "taskId", "taskActivityId");
  }

  @Test
  public void testWatchConfigMap() throws ApiException {
    stubFor(get(urlPathMatching("/api/v1/namespaces/default/configmaps"))
        .willReturn(okForContentType(MediaType.APPLICATION_JSON_VALUE,
            "{\"type\": \"type\", \"object\": {\"apiVersion\": \"1.0\", \"metadata\": {\"name\": \"configMapMetadataName\"}, \"items\": [{\"metadata\": {\"name\": \"metadataName\"}}]}}")));

    V1ConfigMap configMap =
        flowKubeService.watchConfigMap("workflowId", "workflowActivityId", "taskId");

    assertNotNull(configMap);
  }

  @Test(expected = KubeRuntimeException.class)
  public void testWatchConfigMapWithException() throws ApiException {
    stubFor(get(urlPathMatching("/api/v1/namespaces/default/configmaps")).willReturn(aResponse()
        .withStatus(404).withHeader("Content-Type", "text/plain").withBody("Not Found")));

    flowKubeService.watchConfigMap("workflowId", "workflowActivityId", "taskId");
  }
}
