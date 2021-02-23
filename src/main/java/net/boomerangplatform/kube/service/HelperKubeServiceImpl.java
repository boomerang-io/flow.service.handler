package net.boomerangplatform.kube.service;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import io.kubernetes.client.models.V1Affinity;
import io.kubernetes.client.models.V1EnvVar;
import io.kubernetes.client.models.V1LabelSelector;
import io.kubernetes.client.models.V1ObjectMeta;
import io.kubernetes.client.models.V1PodAffinityTerm;
import io.kubernetes.client.models.V1PodAntiAffinity;
import io.kubernetes.client.models.V1PodSpec;
import io.kubernetes.client.models.V1Toleration;
import io.kubernetes.client.models.V1WeightedPodAffinityTerm;
import net.boomerangplatform.model.TaskConfiguration;
import net.boomerangplatform.service.ConfigurationService;

@Component
public class HelperKubeServiceImpl implements HelperKubeService {

  private static final Logger LOGGER = LogManager.getLogger(HelperKubeService.class);
  
  protected static final String TIER = "worker";

  private static final String EXCEPTION = "Exception: ";

  @Value("${proxy.enable}")
  protected Boolean proxyEnabled;

  @Value("${proxy.host}")
  protected String proxyHost;

  @Value("${proxy.port}")
  protected String proxyPort;

  @Value("${proxy.ignore}")
  protected String proxyIgnore;
  
  @Value("${boomerang.product:bmrg-flow}")
  protected String bmrgProduct;

  @Autowired
  private ConfigurationService configurationService;

//  Utilized by LogServiceImpl
  @Override
  public String getPrefixJob() {
    return bmrgProduct + "-" + TIER;
  }
  
  protected String getPrefixPVC() {
    return bmrgProduct + "-pvc";
  }
  
  protected String getPrefixPV() {
    return bmrgProduct + "-pv";
  }
  
  protected String getPrefixCFGMAP() {
    return bmrgProduct + "-cfg";
  }
  
  protected String getPrefixVol() {
    return bmrgProduct + "-vol";
  }

  protected List<V1EnvVar> createProxyEnvVars() {
    List<V1EnvVar> proxyEnvVars = new ArrayList<>();

    final String proxyUrl = "http://" + proxyHost + ":" + proxyPort;
    proxyEnvVars.add(createEnvVar("PROXY_HOST", proxyHost));
    proxyEnvVars.add(createEnvVar("PROXY_PORT", proxyPort));
    proxyEnvVars.add(createEnvVar("HTTP_PROXY", proxyUrl));
    proxyEnvVars.add(createEnvVar("HTTPS_PROXY", proxyUrl));
    proxyEnvVars.add(createEnvVar("http_proxy", proxyUrl));
    proxyEnvVars.add(createEnvVar("https_proxy", proxyUrl));
    proxyEnvVars.add(createEnvVar("NO_PROXY", proxyIgnore));
    proxyEnvVars.add(createEnvVar("no_proxy", proxyIgnore));
    proxyEnvVars.add(createEnvVar("use_proxy", "on"));

    return proxyEnvVars;
  }

  protected V1EnvVar createEnvVar(String key, String value) {
    V1EnvVar envVar = new V1EnvVar();
    envVar.setName(key);
    envVar.setValue(value);
    return envVar;
  }

  protected String createConfigMapProp(Map<String, String> properties) {
    LOGGER.info("Building ConfigMap Body");
    Properties props = new Properties();
    StringWriter propsSW = new StringWriter();
    if (properties != null && !properties.isEmpty()) {
      properties.forEach((key, value) -> {
        String valueStr = value != null ? value : "";
        LOGGER.info("createConfigMapProp() - " + key + "=" + valueStr);
        props.setProperty(key, valueStr);
      });
    }

    try {
      props.store(propsSW, null);
      LOGGER.info("" + propsSW.toString());
    } catch (IOException ex) {
      LOGGER.error(EXCEPTION, ex);
    }

    return propsSW.toString();
  }
  
  protected Map<String, String> getConfigMapProp(String properties) throws IOException {
    LOGGER.info("Retrieving ConfigMap Properties File");
    Properties props = new Properties();
    if (properties != null && !properties.isEmpty()) {
      props.load(new StringReader(properties));
    }
    props.list(System.out);
    Map<String, String> propsMap = new HashMap<>();
    for (Map.Entry<Object, Object> e : props.entrySet()) {
      String key = (String)e.getKey();
      String value = (String)e.getValue();
      propsMap.put(key, value);
    }
    return propsMap;
  }

  /*
   * Passes through optional method inputs to the sub methods which need to handle this.
   */
  protected V1ObjectMeta getMetadata(String workflowName, String workflowId,
      String activityId, String taskId, String generateName, Map<String, String> labels) {
    V1ObjectMeta metadata = new V1ObjectMeta();
    metadata.annotations(createAnnotations(workflowName, workflowId, activityId, taskId));
    metadata.labels(createLabels(workflowId, activityId, taskId, labels));
    if (StringUtils.isNotBlank(generateName)) {
      metadata.generateName(generateName + "-");
    }
    return metadata;
  }

  /*
   * Sets the tolerations and nodeSelector to match the dedicated node taints and node-role label
   */
  protected void getTolerationAndSelector(V1PodSpec podSpec) {
    V1Toleration nodeTolerationItem = new V1Toleration();
    nodeTolerationItem.key("dedicated");
    nodeTolerationItem.value("bmrg-worker");
    nodeTolerationItem.effect("NoSchedule");
    nodeTolerationItem.operator("Equal");
    podSpec.addTolerationsItem(nodeTolerationItem);
    podSpec.putNodeSelectorItem("node-role.kubernetes.io/bmrg-worker", "true");
  }

  /*
   * Sets the pod anti affinity
   */
  protected void getPodAntiAffinity(V1PodSpec podSpec, Map<String, String> labels) {
    V1LabelSelector labelSelector = new V1LabelSelector();
    labelSelector.setMatchLabels(labels);
    V1PodAffinityTerm podAntiAffinityPreferredTerm = new V1PodAffinityTerm();
    podAntiAffinityPreferredTerm.setLabelSelector(labelSelector);
    podAntiAffinityPreferredTerm.setTopologyKey("kubernetes.io/hostname");
    V1WeightedPodAffinityTerm podAntiAffinityPreferred = new V1WeightedPodAffinityTerm();
    podAntiAffinityPreferred.setWeight(100);
    podAntiAffinityPreferred.setPodAffinityTerm(podAntiAffinityPreferredTerm);
    V1PodAntiAffinity podAntiAffinity = new V1PodAntiAffinity();
    podAntiAffinity
        .addPreferredDuringSchedulingIgnoredDuringExecutionItem(podAntiAffinityPreferred);
    V1Affinity podAffinity = new V1Affinity();
    podAffinity.setPodAntiAffinity(podAntiAffinity);
    podSpec.affinity(podAffinity);
  }

  protected Map<String, String> createAntiAffinityLabels() {
    Map<String, String> labels = new HashMap<>();
    labels.put("boomerang.io/product", bmrgProduct);
    labels.put("boomerang.io/tier", TIER);
    return labels;
  }

  protected Map<String, String> createLabels(String workflowId, String activityId, String taskId, Map<String, String> customLabels) {
    Map<String, String> labels = new HashMap<>();
    labels.put("app.kubernetes.io/name", TIER);
    labels.put("app.kubernetes.io/instance", TIER + "-" + workflowId);
    labels.put("app.kubernetes.io/part-of", bmrgProduct);
    labels.put("app.kubernetes.io/managed-by", "controller");
    labels.put("boomerang.io/product", bmrgProduct);
    labels.put("boomerang.io/tier", TIER);
    Optional.ofNullable(workflowId).ifPresent(str -> labels.put("boomerang.io/workflow-id", str));
    Optional.ofNullable(activityId).ifPresent(str -> labels.put("boomerang.io/activity-id", str));
    Optional.ofNullable(taskId).ifPresent(str -> labels.put("boomerang.io/task-id", str));
    Optional.ofNullable(customLabels).ifPresent(lbl -> labels.putAll(lbl));
    return labels;
  }
  
  protected Map<String, String> createWorkspaceLabels(String workspaceId, Map<String, String> customLabels) {
    Map<String, String> labels = new HashMap<>();
    labels.put("app.kubernetes.io/name", TIER);
    labels.put("app.kubernetes.io/instance", TIER + "-" + workspaceId);
    labels.put("app.kubernetes.io/part-of", bmrgProduct);
    labels.put("app.kubernetes.io/managed-by", "controller");
    labels.put("boomerang.io/product", bmrgProduct);
    labels.put("boomerang.io/tier", TIER);
    labels.put("boomerang.io/workspace-id", workspaceId);
    Optional.ofNullable(customLabels).ifPresent(lbl -> labels.putAll(lbl));
    return labels;
  }

  protected Map<String, String> createAnnotations(String workflowName, String workflowId,
      String activityId, String taskId) {
    Map<String, String> annotations = new HashMap<>();
    annotations.put("boomerang.io/workflow-name", workflowName);
    annotations.put("boomerang.io/selector", getLabelSelector(workflowId, activityId, taskId));
    return annotations;
  }
  
  protected Map<String, String> createWorkspaceAnnotations(String workspaceName, String workspaceId) {
    Map<String, String> annotations = new HashMap<>();
    annotations.put("boomerang.io/workspace-name", workspaceName);
    annotations.put("boomerang.io/selector", getWorkspaceLabelSelector(workspaceId));
    return annotations;
  }

  protected String getLabelSelector(String workflowId, String activityId, String taskId) {
    StringBuilder labelSelector = new StringBuilder("boomerang.io/product=" + bmrgProduct + ",boomerang.io/tier=" + TIER);
    Optional.ofNullable(workflowId).ifPresent(str -> labelSelector.append(",boomerang.io/workflow-id=" + str));
    Optional.ofNullable(activityId).ifPresent(str -> labelSelector.append(",boomerang.io/activity-id=" + str));
    Optional.ofNullable(taskId).ifPresent(str -> labelSelector.append(",boomerang.io/task-id=" + str));

    LOGGER.info("  labelSelector: " + labelSelector.toString());
    return labelSelector.toString();
  }
  
  protected String getWorkspaceLabelSelector(String workspaceId) {
    StringBuilder labelSelector = new StringBuilder("boomerang.io/product=" + bmrgProduct + ",boomerang.io/tier=" + TIER + ",boomerang.io/workspace-id=" + workspaceId);

    LOGGER.info("  labelSelector: " + labelSelector.toString());
    return labelSelector.toString();
  }

  protected List<V1EnvVar> createEnvVars(String workflowId,String activityId,String taskName,String taskId){
      List<V1EnvVar> envVars = new ArrayList<>();
      envVars.add(createEnvVar("BMRG_WORKFLOW_ID", workflowId));
      envVars.add(createEnvVar("BMRG_ACTIVITY_ID", activityId));
      envVars.add(createEnvVar("BMRG_TASK_ID", taskId));
      envVars.add(createEnvVar("BMRG_TASK_NAME", taskName.replace(" ", "")));
      return envVars;
  }

  protected String getTaskDebug(TaskConfiguration taskConfiguration) {
    return taskConfiguration != null && taskConfiguration.getDebug() != null ? taskConfiguration.getDebug().toString()
        : configurationService.getTaskDebug().toString();
  }
}
