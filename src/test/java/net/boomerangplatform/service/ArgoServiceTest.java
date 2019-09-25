package net.boomerangplatform.service;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.okForContentType;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import io.kubernetes.client.ApiClient;
import io.kubernetes.client.ApiException;
import io.kubernetes.client.Configuration;
import io.kubernetes.client.util.ClientBuilder;
import net.boomerangplatform.TestUtil;
import net.boomerangplatform.model.argo.Annotations;
import net.boomerangplatform.model.argo.Arguments;
import net.boomerangplatform.model.argo.BmrgFlowV1;
import net.boomerangplatform.model.argo.BmrgFlowV2;
import net.boomerangplatform.model.argo.BmrgFlowV3;
import net.boomerangplatform.model.argo.BmrgFlowV4;
import net.boomerangplatform.model.argo.GeneralProperties;
import net.boomerangplatform.model.argo.ImagePullSecret;
import net.boomerangplatform.model.argo.Inputs;
import net.boomerangplatform.model.argo.Labels;
import net.boomerangplatform.model.argo.Metadata;
import net.boomerangplatform.model.argo.Nodes;
import net.boomerangplatform.model.argo.Outputs;
import net.boomerangplatform.model.argo.Script;
import net.boomerangplatform.model.argo.Spec;
import net.boomerangplatform.model.argo.Status;
import net.boomerangplatform.model.argo.Step;
import net.boomerangplatform.model.argo.Template;
import net.boomerangplatform.model.argo.Workflow;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest
@ActiveProfiles("local")
public class ArgoServiceTest {

  private ArgoServiceImpl argoService;

  private ApiClient client;

  private static final int PORT = 8089;
  @Rule
  public WireMockRule wireMockRule = new WireMockRule(PORT);

  @Before
  public void setup() throws IOException {
    argoService = new ArgoServiceImpl();
    client = new ClientBuilder().setBasePath("http://localhost:" + PORT).build();
    Configuration.setDefaultApiClient(client);
    argoService.setApiClient(client);
  }

  @Test
  public void testGetWorkflowWithError() throws ApiException {
    stubFor(get(urlPathEqualTo("/apis/argoproj.io/v1alpha1/namespaces/default/workflows/podName"))
        .willReturn(aResponse().withStatus(404).withHeader("Content-Type", "text/plain")
            .withBody("Not Found")));

    assertNull(argoService.getWorkflow("podName"));
  }

  @Test
  public void testGetWorkflow() throws ApiException {
    stubFor(get(urlPathEqualTo("/apis/argoproj.io/v1alpha1/namespaces/default/workflows/name"))
        .willReturn(okForContentType(MediaType.APPLICATION_JSON_VALUE,
            TestUtil.loadResourceAsString("response.json"))));
    Workflow workflow = argoService.getWorkflow("name");
    assertNotNull(workflow);

    validateWorkflow(workflow);
  }

  @Test
  public void testGetWorkflowWithNull() throws ApiException {
    stubFor(get(urlPathEqualTo("/apis/argoproj.io/v1alpha1/namespaces/default/workflows/podName"))
        .willReturn(okForContentType(MediaType.APPLICATION_JSON_VALUE, "")));

    assertNull(argoService.getWorkflow("podName"));
  }

  @Test
  public void testCreateWorkflowWithError() throws ApiException {
    Workflow workflow = getDefaultWorkflow();

    stubFor(post(urlPathEqualTo("/apis/argoproj.io/v1alpha1/namespaces/default/workflows"))
        .willReturn(aResponse().withStatus(404).withHeader("Content-Type", "text/plain")
            .withBody("Not Found")));

    assertNull(argoService.createWorkflow(workflow));
  }

  @Test
  public void testCreateWorkflow() throws ApiException {
    Workflow workflow = new Workflow();
    workflow.setApiVersion("1.0");

    stubFor(
        post(urlPathEqualTo("/apis/argoproj.io/v1alpha1/namespaces/default/workflows")).willReturn(
            okForContentType(MediaType.APPLICATION_JSON_VALUE, "{\"apiVersion\": \"1.0\"}")));

    Object createdWorkflow = argoService.createWorkflow(workflow);
    assertNotNull(createdWorkflow);
  }

  private Workflow getDefaultWorkflow() {
    Workflow workflow = new Workflow();
    workflow.setApiVersion("1.0");
    workflow.setKind("kind");
    workflow.setAdditionalProperty("myProp", "myValue");
    Metadata metadata = new Metadata();
    Annotations annotations = new Annotations();
    annotations.setKubectlKubernetesIoLastAppliedConfiguration(
        "kubectlKubernetesIoLastAppliedConfiguration");
    metadata.setAnnotations(annotations);
    metadata.setClusterName("clusterName");
    metadata.setCreationTimestamp(String.valueOf(System.currentTimeMillis()));
    metadata.setGeneration(1);
    Labels labels = new Labels();
    labels.setWorkflowsArgoprojIoCompleted("workflowsArgoprojIoCompleted");
    labels.setWorkflowsArgoprojIoPhase("workflowsArgoprojIoPhase");
    metadata.setLabels(labels);
    metadata.setName("name");
    metadata.setNamespace("namespace");
    metadata.setResourceVersion("resourceVersion");
    metadata.setSelfLink("selfLink");
    metadata.setUid("uid");
    workflow.setMetadata(metadata);
    Spec spec = new Spec();
    Arguments arguments = new Arguments();
    arguments.setAdditionalProperty("name", "value");
    spec.setArguments(arguments);
    spec.setEntrypoint("entrypoint");
    ImagePullSecret imagePullSecret = new ImagePullSecret();
    imagePullSecret.setName("name");
    List<ImagePullSecret> imagePullSecrets = new ArrayList<>();
    imagePullSecrets.add(imagePullSecret);
    spec.setImagePullSecrets(imagePullSecrets);
    Template template = new Template();
    Inputs inputs = new Inputs();
    inputs.setAdditionalProperty("name", "value");
    template.setInputs(inputs);
    template.setMetadata(metadata);
    template.setName("templateName");
    template.setOutputs(new GeneralProperties());
    Script script = new Script();
    script.setName("name");
    script.setCommand(Arrays.asList("command1"));
    script.setImage("imageName");
    script.setResources(new GeneralProperties());
    script.setSource("source");
    template.setScript(script);
    Step step = new Step();
    step.setArguments(arguments);
    step.setName("name");
    step.setTemplate("template");
    template.setSteps(Arrays.asList(Arrays.asList(step)));
    spec.setTemplates(Arrays.asList(template));
    workflow.setSpec(spec);

    Status status = new Status();
    status.setPhase("phase");
    status.setFinishedAt("finishedAt");
    status.setStartedAt("startedAt");

    Nodes nodes = new Nodes();
    BmrgFlowV1 bmrgFlow1 = new BmrgFlowV1();
    bmrgFlow1.setId("id");
    bmrgFlow1.setChildren(Arrays.asList("children"));
    bmrgFlow1.setDisplayName("displayName");
    bmrgFlow1.setFinishedAt("finishedAt");
    bmrgFlow1.setName("name");
    bmrgFlow1.setOutboundNodes(Arrays.asList("outboundNode"));
    bmrgFlow1.setPhase("phase");
    bmrgFlow1.setStartedAt("startedAt");
    bmrgFlow1.setTemplateName("templateName");
    bmrgFlow1.setType("type");
    nodes.setBmrgFlow1(bmrgFlow1);
    BmrgFlowV2 bmrgFlow2 = new BmrgFlowV2();
    Outputs outputs = new Outputs();
    outputs.setResult("result");
    bmrgFlow2.setOutputs(outputs);
    bmrgFlow2.setBoundaryID("boundaryID");
    bmrgFlow2.setTemplateName("templateName");
    nodes.setBmrgFlow115782439(bmrgFlow2);
    BmrgFlowV3 bmrgFlow3 = new BmrgFlowV3();
    bmrgFlow3.setBoundaryID("boundaryID");
    bmrgFlow3.setTemplateName("templateName");
    bmrgFlow3.setOutputs(outputs);
    bmrgFlow3.setChildren(Arrays.asList("children"));

    nodes.setBmrgFlow14081481(bmrgFlow3);
    BmrgFlowV4 bmrgFlow4 = new BmrgFlowV4();
    bmrgFlow4.setBoundaryID("boundaryID");
    bmrgFlow4.setChildren(Arrays.asList("children"));
    nodes.setBmrgFlow1548609519(bmrgFlow4);
    nodes.setBmrgFlow1615867090(bmrgFlow4);

    status.setNodes(nodes);
    workflow.setStatus(status);
    return workflow;
  }

  public void validateWorkflow(Workflow workflow) {
    assertEquals("1.0", workflow.getApiVersion());
    assertEquals("kind", workflow.getKind());
    Metadata metadata = workflow.getMetadata();
    assertNotNull(metadata);
    assertEquals("kubectlKubernetesIoLastAppliedConfiguration",
        metadata.getAnnotations().getKubectlKubernetesIoLastAppliedConfiguration());
    assertEquals("clusterName", metadata.getClusterName());
    assertEquals("creationTimestamp", metadata.getCreationTimestamp());
    assertTrue(metadata.getGeneration() == 1);
    assertEquals("workflowsArgoprojIoCompleted",
        metadata.getLabels().getWorkflowsArgoprojIoCompleted());
    assertEquals("workflowsArgoprojIoPhase", metadata.getLabels().getWorkflowsArgoprojIoPhase());
    assertEquals("podName", metadata.getName());
    assertEquals("podNamespace", metadata.getNamespace());
    assertEquals("resourceVersion", metadata.getResourceVersion());
    assertEquals("selfLink", metadata.getSelfLink());
    assertEquals("1", metadata.getUid());

    Spec spec = workflow.getSpec();
    assertNotNull(spec.getArguments().getAdditionalProperties().get("myArg"));
    assertEquals("entrypoint", spec.getEntrypoint());
    assertFalse(spec.getImagePullSecrets().isEmpty());
    ImagePullSecret imagePullSecret = spec.getImagePullSecrets().get(0);
    assertEquals("name", imagePullSecret.getName());
    assertFalse(spec.getTemplates().isEmpty());
    Template template = spec.getTemplates().get(0);
    assertEquals("myValue1", template.getInputs().getAdditionalProperties().get("myArg1"));
    assertEquals("myValue2", template.getMetadata().getAdditionalProperties().get("myArg2"));
    assertEquals("specName", template.getName());
    assertEquals("myValue3", template.getOutputs().getAdditionalProperties().get("myArg3"));
    assertFalse(template.getSteps().isEmpty());
    assertFalse(template.getSteps().get(0).isEmpty());
    Step step = template.getSteps().get(0).get(0);
    assertEquals("myValue5", step.getArguments().getAdditionalProperties().get("myArg5"));
    assertEquals("stepName", step.getName());
    assertEquals("stepTemplate", step.getTemplate());


    Script script = template.getScript();
    assertNotNull(script);
    assertTrue(script.getCommand().contains("command2"));
    assertEquals("image", script.getImage());
    assertEquals("scriptName", script.getName());
    assertNotNull(script.getResources());
    assertEquals("myValue4", script.getResources().getAdditionalProperties().get("myArg4"));
    assertEquals("source", script.getSource());
    Status status = workflow.getStatus();
    assertNotNull(status);

    assertEquals("finishedAt", status.getFinishedAt());
    Nodes nodes = status.getNodes();
    assertNotNull(nodes);
    BmrgFlowV1 bmrgFlow1 = nodes.getBmrgFlow1();
    assertNotNull(bmrgFlow1);
    assertEquals("bmrg-flow-1", bmrgFlow1.getDisplayName());
    assertTrue(bmrgFlow1.getChildren().contains("children"));
    assertEquals("finishedAt", bmrgFlow1.getFinishedAt());
    assertEquals("1", bmrgFlow1.getId());
    assertEquals("bmrg-flow-1", bmrgFlow1.getName());
    assertTrue(bmrgFlow1.getOutboundNodes().contains("outboundNode2"));
    assertEquals("phase1", bmrgFlow1.getPhase());
    assertEquals("startedAt", bmrgFlow1.getStartedAt());
    assertEquals("templateName1", bmrgFlow1.getTemplateName());
    assertEquals("type", bmrgFlow1.getType());

    BmrgFlowV2 bmrgFlow2 = nodes.getBmrgFlow115782439();
    assertNotNull(bmrgFlow2);
    assertEquals("boundaryID", bmrgFlow2.getBoundaryID());
    assertEquals("result", bmrgFlow2.getOutputs().getResult());
    assertEquals("templateName2", bmrgFlow2.getTemplateName());
    BmrgFlowV3 bmrgFlow3 = nodes.getBmrgFlow14081481();
    assertNotNull(bmrgFlow3);
    assertEquals("boundaryID", bmrgFlow3.getBoundaryID());
    assertEquals("result", bmrgFlow3.getOutputs().getResult());
    assertEquals("templateName3", bmrgFlow3.getTemplateName());
    assertTrue(bmrgFlow3.getChildren().contains("children"));
    BmrgFlowV4 bmrgFlow4 = nodes.getBmrgFlow1548609519();
    assertNotNull(bmrgFlow4);
    assertEquals("boundaryID", bmrgFlow4.getBoundaryID());
    assertTrue(bmrgFlow4.getChildren().contains("children"));
    assertNotNull(nodes.getBmrgFlow1615867090());
    assertEquals("phase", status.getPhase());
    assertEquals("startedAt", status.getStartedAt());

  }
}
