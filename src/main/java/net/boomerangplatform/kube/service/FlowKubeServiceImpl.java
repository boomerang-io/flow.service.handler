package net.boomerangplatform.kube.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import io.kubernetes.client.models.V1ConfigMap;
import io.kubernetes.client.models.V1Container;
import io.kubernetes.client.models.V1EnvVar;
import io.kubernetes.client.models.V1Job;
import io.kubernetes.client.models.V1JobSpec;
import io.kubernetes.client.models.V1LocalObjectReference;
import io.kubernetes.client.models.V1PersistentVolumeClaimVolumeSource;
import io.kubernetes.client.models.V1PodSpec;
import io.kubernetes.client.models.V1PodTemplateSpec;
import io.kubernetes.client.models.V1ProjectedVolumeSource;
import io.kubernetes.client.models.V1Volume;
import io.kubernetes.client.models.V1VolumeProjection;

@Service
@Profile("live")
public class FlowKubeServiceImpl extends AbstractKubeServiceImpl {

  protected static final String ORG = "bmrg";

  protected static final String PREFIX = ORG + "-cicd";

  protected static final String PREFIX_JOB = PREFIX + "-worker";

  protected static final String PREFIX_CFGMAP = PREFIX + "-cfg";

  protected static final String PREFIX_VOL = PREFIX + "-vol";

  protected static final String PREFIX_VOL_DATA = PREFIX_VOL + "-data";

  protected static final String PREFIX_VOL_PROPS = PREFIX_VOL + "-props";

  private static final String PREFIX_PVC = PREFIX + "-pvc-";

  private static final Logger LOGGER = LogManager.getLogger(FlowKubeServiceImpl.class);

  @Override
  public String getPrefixJob() {
    return PREFIX_JOB;
  }
  
  @Override
  public String getPrefixPVC() {
    return PREFIX_PVC;
  }

  @Override
  protected V1Job createJobBody(String workflowName, String workflowId, String activityId,
      String taskName, String taskId, List<String> arguments,
      Map<String, String> taskInputProperties) {

    // Initialize Job Body
    V1Job body = new V1Job();
    body.metadata(
        getMetadata(workflowName, workflowId, activityId, taskId, getPrefixJob() + "-", true));

    // Create Spec
    V1JobSpec jobSpec = new V1JobSpec();
    V1PodTemplateSpec templateSpec = new V1PodTemplateSpec();
    V1PodSpec podSpec = new V1PodSpec();
    V1Container container = getContainer();

    List<V1EnvVar> envVars = new ArrayList<>();
    if (proxyEnabled) {
      envVars.addAll(createProxyEnvVars());
    }
    envVars.add(createEnvVar("DEBUG", kubeWorkerDebug.toString()));
    container.env(envVars);
    container.args(arguments);
    if (!getPVCName(workflowId, activityId).isEmpty()) {
      container.addVolumeMountsItem(getVolumeMount(PREFIX_VOL_DATA, "/" + "cache"));
      V1Volume workerVolume = getVolume(PREFIX_VOL_DATA);
      V1PersistentVolumeClaimVolumeSource workerVolumePVCSource =
          new V1PersistentVolumeClaimVolumeSource();
      workerVolume.persistentVolumeClaim(
          workerVolumePVCSource.claimName(getPVCName(workflowId, activityId)));
      podSpec.addVolumesItem(workerVolume);
    }

    container.addVolumeMountsItem(getVolumeMount(PREFIX_VOL_PROPS, "/" + "props"));

    // Creation of Projected Volume with multiple ConfigMaps
    V1Volume volumeProps = getVolume(PREFIX_VOL_PROPS);
    V1ProjectedVolumeSource projectedVolPropsSource = new V1ProjectedVolumeSource();
    List<V1VolumeProjection> projectPropsVolumeList = new ArrayList<>();

    // Add Worfklow Configmap Projected Volume
    V1ConfigMap wfConfigMap = getConfigMap(workflowId, activityId, null);
    if (wfConfigMap != null && !getConfigMapName(wfConfigMap).isEmpty()) {
      projectPropsVolumeList.add(getVolumeProjection(wfConfigMap));
    }

    // Add Task Configmap Projected Volume
    V1ConfigMap taskConfigMap = getConfigMap(null, activityId, taskId);
    if (taskConfigMap != null && !getConfigMapName(taskConfigMap).isEmpty()) {
      projectPropsVolumeList.add(getVolumeProjection(taskConfigMap));
    }

    // Add all configmap projected volume
    projectedVolPropsSource.sources(projectPropsVolumeList);
    volumeProps.projected(projectedVolPropsSource);
    podSpec.addVolumesItem(volumeProps);

    if (!kubeWorkerServiceAccount.isEmpty()) {
      podSpec.serviceAccountName(kubeWorkerServiceAccount);
    }

    List<V1Container> containerList = new ArrayList<>();
    containerList.add(container);
    podSpec.containers(containerList);
    V1LocalObjectReference imagePullSecret = new V1LocalObjectReference();
    imagePullSecret.name(kubeImagePullSecret);
    List<V1LocalObjectReference> imagePullSecretList = new ArrayList<>();
    imagePullSecretList.add(imagePullSecret);
    podSpec.imagePullSecrets(imagePullSecretList);
    podSpec.restartPolicy(kubeWorkerJobRestartPolicy);
    templateSpec.spec(podSpec);
    templateSpec.metadata(getMetadata(workflowName, workflowId, activityId, taskId, null, true));

    jobSpec.backoffLimit(kubeWorkerJobBackOffLimit);
    jobSpec.template(templateSpec);
    Integer ttl = ONE_DAY_IN_SECONDS * kubeWorkerJobTTLDays;
    LOGGER.info("Setting Job TTL at " + ttl + " seconds");
    jobSpec.setTtlSecondsAfterFinished(ttl);
    body.spec(jobSpec);

    return body;
  }

  protected V1ConfigMap createTaskConfigMapBody(String workflowName, String workflowId,
      String workflowActivityId, String taskName, String taskId, Map<String, String> inputProps) {
    V1ConfigMap body = new V1ConfigMap();

    body.metadata(
        getMetadata(workflowName, workflowId, workflowActivityId, taskId, PREFIX_CFGMAP, false));

    // Create Data
    Map<String, String> inputsWithFixedKeys = new HashMap<>();
    Map<String, String> sysProps = new HashMap<>();
    sysProps.put("task.id", taskId);
    sysProps.put("task.name", taskName);
    inputsWithFixedKeys.put("task.input.properties", createConfigMapProp(inputProps));
    inputsWithFixedKeys.put("task.system.properties", createConfigMapProp(sysProps));
    body.data(inputsWithFixedKeys);
    return body;
  }

  protected V1ConfigMap createWorkflowConfigMapBody(String workflowName, String workflowId,
      String workflowActivityId, Map<String, String> inputProps) {
    V1ConfigMap body = new V1ConfigMap();

    body.metadata(
        getMetadata(workflowName, workflowId, workflowActivityId, null, PREFIX_CFGMAP, false));

    // Create Data
    Map<String, String> inputsWithFixedKeys = new HashMap<>();
    Map<String, String> sysProps = new HashMap<>();
    sysProps.put("activity.id", workflowActivityId);
    sysProps.put("workflow.name", workflowName);
    sysProps.put("workflow.id", workflowId);
    sysProps.put("worker.debug", kubeWorkerDebug.toString());
    sysProps.put("controller.service.url", bmrgControllerServiceURL);
    inputsWithFixedKeys.put("workflow.input.properties", createConfigMapProp(inputProps));
    inputsWithFixedKeys.put("workflow.system.properties", createConfigMapProp(sysProps));
    body.data(inputsWithFixedKeys);
    return body;
  }


  protected String getLabelSelector(String workflowId, String activityId, String taskId) {
    StringBuilder labelSelector = new StringBuilder("platform=" + ORG + ",app=" + PREFIX);
    Optional.ofNullable(workflowId).ifPresent(str -> labelSelector.append(",workflow-id=" + str));
    Optional.ofNullable(activityId).ifPresent(str -> labelSelector.append(",activity-id=" + str));
    Optional.ofNullable(taskId).ifPresent(str -> labelSelector.append(",task-id=" + str));

    LOGGER.info("  labelSelector: " + labelSelector.toString());
    return labelSelector.toString();
  }

  protected Map<String, String> createAnnotations(String workflowName, String workflowId,
      String activityId, String taskId) {
    Map<String, String> annotations = new HashMap<>();
    annotations.put("boomerangplatform.net/platform", ORG);
    annotations.put("boomerangplatform.net/app", PREFIX);
    annotations.put("boomerangplatform.net/workflow-name", workflowName);
    annotations.put("boomerangplatform.net/workflow-id", workflowId);
    annotations.put("boomerangplatform.net/activity-id", activityId);
    Optional.ofNullable(taskId)
        .ifPresent(str -> annotations.put("boomerangplatform.net/task-id", str));

    return annotations;
  }

  protected Map<String, String> createLabels(String workflowId, String activityId, String taskId) {
    Map<String, String> labels = new HashMap<>();
    labels.put("platform", ORG);
    labels.put("app", PREFIX);
    Optional.ofNullable(workflowId).ifPresent(str -> labels.put("workflow-id", str));
    Optional.ofNullable(activityId).ifPresent(str -> labels.put("activity-id", str));
    Optional.ofNullable(taskId).ifPresent(str -> labels.put("task-id", str));
    return labels;
  }

}
