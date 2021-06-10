package net.boomerangplatform.kube.service;

import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import io.fabric8.kubernetes.api.model.Affinity;
import io.fabric8.kubernetes.api.model.EnvVar;
import io.fabric8.kubernetes.api.model.LabelSelector;
import io.fabric8.kubernetes.api.model.PodAffinityTerm;
import io.fabric8.kubernetes.api.model.PodAntiAffinity;
import io.fabric8.kubernetes.api.model.WeightedPodAffinityTerm;
import net.boomerangplatform.model.TaskConfiguration;

@Component
public class NewHelperKubeServiceImpl {

  private static final Logger LOGGER = LogManager.getLogger(NewHelperKubeServiceImpl.class);

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
  
  @Value("${boomerang.instance:bmrg-flow}")
  protected String bmrgInstance;
  
  // Utilized by LogServiceImpl
  // @Override
  public String getPrefixTask() {
    return bmrgProduct + "-task";
  }

  protected String getPrefixPVC() {
    return bmrgProduct + "-pvc";
  }

  protected String getPrefixPV() {
    return bmrgProduct + "-pv";
  }

  protected String getPrefixCM() {
    return bmrgProduct + "-cfg";
  }

  protected String getPrefixVol() {
    return bmrgProduct + "-vol";
  }
  
  protected List<EnvVar> createProxyEnvVars() {
    List<EnvVar> proxyEnvVars = new ArrayList<>();

    if (proxyEnabled) {
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
    }

    return proxyEnvVars;
  }

  protected EnvVar createEnvVar(String key, String value) {
    EnvVar envVar = new EnvVar();
    envVar.setName(key);
    envVar.setValue(value);
    return envVar;
  }

  protected List<EnvVar> createEnvVars(String workflowId, String workflowActivityId,
      String taskName, String taskId, String taskActivityId) {
    List<EnvVar> envVars = new ArrayList<>();
    envVars.add(createEnvVar("BMRG_WORKFLOW_ID", workflowId));
    envVars.add(createEnvVar("BMRG_WORKFLOW_ACTIVITY_ID", workflowActivityId));
    envVars.add(createEnvVar("BMRG_TASK_ID", taskId));
    envVars.add(createEnvVar("BMRG_TASK_ACTIVITY_ID", taskActivityId));
    envVars.add(createEnvVar("BMRG_TASK_NAME", taskName.replace(" ", "")));
    return envVars;
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
      LOGGER.error(ex);
    }

    return propsSW.toString();
  }
  //
  // protected Map<String, String> getConfigMapProp(String properties) throws IOException {
  // LOGGER.info("Retrieving ConfigMap Properties File");
  // Properties props = new Properties();
  // if (properties != null && !properties.isEmpty()) {
  // props.load(new StringReader(properties));
  // }
  // props.list(System.out);
  // Map<String, String> propsMap = new HashMap<>();
  // for (Map.Entry<Object, Object> e : props.entrySet()) {
  // String key = (String)e.getKey();
  // String value = (String)e.getValue();
  // propsMap.put(key, value);
  // }
  // return propsMap;
  // }
  //
  // /*
  // * Passes through optional method inputs to the sub methods which need to handle this.
  // */
  // protected V1ObjectMeta getMetadata(String tier, String workflowName, String workflowId,
  // String workflowActivityId, String taskId, String taskActivityId, String generateName,
  // Map<String, String> labels) {
  // V1ObjectMeta metadata = new V1ObjectMeta();
  // metadata.annotations(createAnnotations(tier, workflowName, workflowId, workflowActivityId,
  // taskId, taskActivityId));
  // metadata.labels(createLabels(tier, workflowId, workflowActivityId, taskId, taskActivityId,
  // labels));
  // if (StringUtils.isNotBlank(generateName)) {
  // metadata.generateName(generateName + "-");
  // }
  // return metadata;
  // }
  //
  // /*
  // * Sets the tolerations and nodeSelector to match the dedicated node taints and node-role label
  // */
  // protected void getTolerationAndSelector(V1PodSpec podSpec) {
  // V1Toleration nodeTolerationItem = new V1Toleration();
  // nodeTolerationItem.key("dedicated");
  // nodeTolerationItem.value("bmrg-worker");
  // nodeTolerationItem.effect("NoSchedule");
  // nodeTolerationItem.operator("Equal");
  // podSpec.addTolerationsItem(nodeTolerationItem);
  // podSpec.putNodeSelectorItem("node-role.kubernetes.io/bmrg-worker", "true");
  // }
  //
   /*
   * Sets the pod anti affinity
   */
   protected Affinity getPodAffinity(Map<String, String> labels) {
     Affinity affinity = new Affinity();
     PodAntiAffinity podAntiAffinity = new PodAntiAffinity();
     List<WeightedPodAffinityTerm> weightedPodAffinityTerms = new ArrayList<>();
     LabelSelector labelSelector = new LabelSelector();
     labelSelector.setMatchLabels(labels);
     PodAffinityTerm podAffinityTerm = new PodAffinityTerm();
     podAffinityTerm.setLabelSelector(labelSelector);
     podAffinityTerm.setTopologyKey("kubernetes.io/hostname");
     WeightedPodAffinityTerm weightedPodAffinityTerm = new WeightedPodAffinityTerm();
     weightedPodAffinityTerm.setWeight(100);
     weightedPodAffinityTerm.setPodAffinityTerm(podAffinityTerm);
     weightedPodAffinityTerms.add(weightedPodAffinityTerm);
     podAntiAffinity.setPreferredDuringSchedulingIgnoredDuringExecution(weightedPodAffinityTerms);
     affinity.setPodAntiAffinity(podAntiAffinity);
     return affinity;
   }
  
  protected Map<String, String> createAntiAffinityLabels(String tier) {
    Map<String, String> labels = new HashMap<>();
    labels.put("boomerang.io/product", bmrgProduct);
    labels.put("boomerang.io/tier", tier);
    return labels;
  }


  private Map<String, String> getLabels(String tier, String workspaceId, String workflowId,
      String workflowActivityId, String taskId, String taskActivityId,
      Map<String, String> customLabels) {
    Map<String, String> labels = new HashMap<>();
    labels.put("app.kubernetes.io/name", bmrgProduct);
    labels.put("app.kubernetes.io/instance", bmrgInstance);
    labels.put("app.kubernetes.io/managed-by", "controller");
    labels.put("boomerang.io/product", bmrgProduct);
    labels.put("boomerang.io/tier", tier);
    Optional.ofNullable(workspaceId)
        .ifPresent(str -> labels.put("boomerang.io/workspace-id", str));
    Optional.ofNullable(workflowId).ifPresent(str -> labels.put("boomerang.io/workflow-id", str));
    Optional.ofNullable(workflowActivityId)
        .ifPresent(str -> labels.put("boomerang.io/workflow-activity-id", str));
    Optional.ofNullable(taskId).ifPresent(str -> labels.put("boomerang.io/task-id", str));
    Optional.ofNullable(taskActivityId)
        .ifPresent(str -> labels.put("boomerang.io/task-activity-id", str));
    Optional.ofNullable(customLabels).ifPresent(lbl -> labels.putAll(lbl));
    return labels;
  }

  protected Map<String, String> getTaskLabels(String workflowId, String workflowActivityId,
      String taskId, String taskActivityId, Map<String, String> customLabels) {
    return getLabels("task", null, workflowId, workflowActivityId, taskId, taskActivityId,
        customLabels);
  }

  protected Map<String, String> getWorkflowLabels(String workflowId, String workflowActivityId,
      Map<String, String> customLabels) {
    return getLabels("workflow", null, workflowId, workflowActivityId, null, null, customLabels);
  }

  protected Map<String, String> getWorkspaceLabels(String workspaceId,
      Map<String, String> customLabels) {
    return getLabels("workspace", workspaceId, null, null, null, null, customLabels);
  }

  protected Map<String, String> getAnnotations(String tier, String workflowName, String workflowId,
      String workflowActivityId, String taskId, String taskActivityId) {
    Map<String, String> annotations = new HashMap<>();
    annotations.put("boomerang.io/workflow-name", workflowName);
    annotations.put("boomerang.io/selector",
        labelSelector(tier, workflowId, workflowActivityId, taskId, taskActivityId));
    return annotations;
  }

  protected Map<String, String> getWorkspaceAnnotations(String workspaceName, String workspaceId) {
    Map<String, String> annotations = new HashMap<>();
    annotations.put("boomerang.io/workspace-name", workspaceName);
    annotations.put("boomerang.io/selector", workspaceLabelSelector(workspaceId));
    return annotations;
  }

  protected String labelSelector(String tier, String workflowId, String workflowActivityId,
      String taskId, String taskActivityId) {
    StringBuilder labelSelector =
        new StringBuilder("boomerang.io/product=" + bmrgProduct + ",boomerang.io/tier=" + tier);
    Optional.ofNullable(workflowId)
        .ifPresent(str -> labelSelector.append(",boomerang.io/workflow-id=" + str));
    Optional.ofNullable(workflowActivityId)
        .ifPresent(str -> labelSelector.append(",boomerang.io/workflow-activity-id=" + str));
    Optional.ofNullable(taskId)
        .ifPresent(str -> labelSelector.append(",boomerang.io/task-id=" + str));
    Optional.ofNullable(taskActivityId)
        .ifPresent(str -> labelSelector.append(",boomerang.io/task-activity-id=" + str));

    LOGGER.info("  labelSelector: " + labelSelector.toString());
    return labelSelector.toString();
  }

  protected String workspaceLabelSelector(String workspaceId) {
    StringBuilder labelSelector = new StringBuilder("boomerang.io/product=" + bmrgProduct
        + ",boomerang.io/tier=workspace,boomerang.io/workspace-id=" + workspaceId);

    LOGGER.info("  labelSelector: " + labelSelector.toString());
    return labelSelector.toString();
  }

  protected String getTaskDebug(TaskConfiguration taskConfiguration) {
    return taskConfiguration != null && taskConfiguration.getDebug() != null && (taskConfiguration.getDebug() ==  Boolean.TRUE || taskConfiguration.getDebug() ==  Boolean.FALSE)
        ? taskConfiguration.getDebug().toString()
        : Boolean.FALSE.toString();
  }
}
