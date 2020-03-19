package net.boomerangplatform.kube.service;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import io.kubernetes.client.custom.Quantity;
import io.kubernetes.client.models.V1ConfigMap;
import io.kubernetes.client.models.V1Container;
import io.kubernetes.client.models.V1EmptyDirVolumeSource;
import io.kubernetes.client.models.V1EnvVar;
import io.kubernetes.client.models.V1HostAlias;
import io.kubernetes.client.models.V1Job;
import io.kubernetes.client.models.V1JobSpec;
import io.kubernetes.client.models.V1LocalObjectReference;
import io.kubernetes.client.models.V1ObjectMeta;
import io.kubernetes.client.models.V1PersistentVolumeClaimVolumeSource;
import io.kubernetes.client.models.V1PodSpec;
import io.kubernetes.client.models.V1PodTemplateSpec;
import io.kubernetes.client.models.V1ProjectedVolumeSource;
import io.kubernetes.client.models.V1ResourceRequirements;
import io.kubernetes.client.models.V1Volume;
import io.kubernetes.client.models.V1VolumeProjection;

@Service
@Profile("cicd")
public class CICDKubeServiceImpl extends AbstractKubeServiceImpl {

  protected static final String ORG = "bmrg";
  
  protected static final String PRODUCT = "cicd";
  
  protected static final String TIER = "worker";

  protected static final String PREFIX = ORG + "-" + PRODUCT;

  protected static final String PREFIX_JOB = PREFIX + "-" + TIER;

  protected static final String PREFIX_CFGMAP = PREFIX + "-cfg";

  protected static final String PREFIX_VOL = PREFIX + "-vol";

  protected static final String PREFIX_VOL_DATA = PREFIX_VOL + "-data";
  
  protected static final String PREFIX_VOL_CACHE = PREFIX_VOL + "-cache";

  protected static final String PREFIX_VOL_PROPS = PREFIX_VOL + "-props";

  private static final String PREFIX_PVC = PREFIX + "-pvc";

  private static final Logger LOGGER = LogManager.getLogger(CICDKubeServiceImpl.class);

  @Value("${kube.api.timeout}")
  private Integer kubeApiTimeOut;
  
  @Value("${kube.resource.limit.ephemeral-storage}")
  private String kubeResourceLimitEphemeralStorage;
  
  @Value("${kube.resource.request.ephemeral-storage}")
  private String kubeResourceRequestEphemeralStorage;
  
  @Value("${kube.worker.storage.data.memory}")
  private Boolean kubeWorkerStorageDataMemory;
  
  @Value("${kube.worker.storage.data.size}")
  private String kubeWorkerStorageDataSize;

  @Override
  public String getPrefixJob() {
    return PREFIX_JOB;
  }
  
  @Override
  public String getPrefixPVC() {
    return PREFIX_PVC;
  }

  /**
 *
 */
@Override
  protected V1Job createJobBody(boolean createLifecycle, String componentName, String componentId, String activityId, String taskActivityId,
      String taskName, String taskId, List<String> arguments,
      Map<String, String> taskProperties, String image, String command) {

    // Initialize Job Body
    V1Job body = new V1Job();
    V1ObjectMeta jobMetadata = getMetadata(componentName, componentId, activityId, taskId, null);
	jobMetadata.name(PREFIX_JOB + "-" + activityId);
    body.metadata(jobMetadata);

    // Create Spec
    V1JobSpec jobSpec = new V1JobSpec();
    V1PodTemplateSpec templateSpec = new V1PodTemplateSpec();
    V1PodSpec podSpec = new V1PodSpec();
    V1Container container = getContainer(image, command);
    List<V1EnvVar> envVars = new ArrayList<>();
    if (proxyEnabled) {
      envVars.addAll(createProxyEnvVars());
    }
    envVars.add(createEnvVar("DEBUG", kubeWorkerDebug.toString()));
    envVars.add(createEnvVar("CI", "true"));
    container.env(envVars);
    container.args(arguments);
    V1ResourceRequirements resources = new V1ResourceRequirements();
    resources.putLimitsItem("ephemeral-storage", new Quantity(kubeResourceLimitEphemeralStorage));
//    resources.putRequestsItem("ephemeral-storage", new Quantity(kubeResourceRequestEphemeralStorage));
    container.setResources(resources);
    if (checkPVCExists(componentId, null, null, true)) {
      container.addVolumeMountsItem(getVolumeMount(PREFIX_VOL_CACHE, "/cache"));
      V1Volume workerVolume = getVolume(PREFIX_VOL_CACHE);
      V1PersistentVolumeClaimVolumeSource workerVolumePVCSource =
          new V1PersistentVolumeClaimVolumeSource();
      workerVolume
          .persistentVolumeClaim(workerVolumePVCSource.claimName(getPVCName(componentId, null)));
      podSpec.addVolumesItem(workerVolume);
    }
    container.addVolumeMountsItem(getVolumeMount(PREFIX_VOL_PROPS, "/props"));

    /*  The following code is integrated to the helm chart and CICD properties
    * 	It allows for containers that breach the standard ephemeral-storage size by off-loading to memory
    * 	See: https://kubernetes.io/docs/concepts/storage/volumes/#emptydir
    */
    container.addVolumeMountsItem(getVolumeMount(PREFIX_VOL_DATA, "/data"));
	V1Volume dataVolume = getVolume(PREFIX_VOL_DATA);
	V1EmptyDirVolumeSource emptyDir = new V1EmptyDirVolumeSource();
    if (kubeWorkerStorageDataMemory && Boolean.valueOf(taskProperties.get("worker.storage.data.memory"))) {
    	LOGGER.info("Setting /data to in memory storage...");
    	emptyDir.setMedium("Memory");
    }
	emptyDir.setSizeLimit(kubeWorkerStorageDataSize);
	dataVolume.emptyDir(emptyDir);
	podSpec.addVolumesItem(dataVolume);

    // Creation of Projected Volume for multiple ConfigMaps
    V1Volume volumeProps = getVolume(PREFIX_VOL_PROPS);
    V1ProjectedVolumeSource projectedVolPropsSource = new V1ProjectedVolumeSource();
    List<V1VolumeProjection> projectPropsVolumeList = new ArrayList<>();

    // Add Worfklow Configmap Projected Volume
    V1ConfigMap wfConfigMap = getConfigMap(componentId, activityId, null);
    if (wfConfigMap != null && !getConfigMapName(wfConfigMap).isEmpty()) {
      projectPropsVolumeList.add(getVolumeProjection(wfConfigMap));
    }
    // Add Task Configmap Projected Volume
    V1ConfigMap taskConfigMap = getConfigMap(componentId, activityId, taskId);
    if (taskConfigMap != null && !getConfigMapName(taskConfigMap).isEmpty()) {
      projectPropsVolumeList.add(getVolumeProjection(taskConfigMap));
    }

    // Add all configmap projected volume
    projectedVolPropsSource.sources(projectPropsVolumeList);
    volumeProps.projected(projectedVolPropsSource);
    podSpec.addVolumesItem(volumeProps);

    List<V1Container> containerList = new ArrayList<>();
    containerList.add(container);
    podSpec.containers(containerList);
    
    if (kubeWorkerDedicatedNodes) {
    	getTolerationAndSelector(podSpec);
    }

    if (!kubeWorkerHostAliases.isEmpty()) {
      Type listHostAliasType = new TypeToken<List<V1HostAlias>>() {}.getType();
      List<V1HostAlias> hostAliasList =
          new Gson().fromJson(kubeWorkerHostAliases, listHostAliasType);
      podSpec.hostAliases(hostAliasList);
    }

    if (!kubeWorkerServiceAccount.isEmpty()) {
      podSpec.serviceAccountName(kubeWorkerServiceAccount);
    }

    V1LocalObjectReference imagePullSecret = new V1LocalObjectReference();
    imagePullSecret.name(kubeImagePullSecret);
    List<V1LocalObjectReference> imagePullSecretList = new ArrayList<>();
    imagePullSecretList.add(imagePullSecret);
    podSpec.imagePullSecrets(imagePullSecretList);
    podSpec.restartPolicy(kubeWorkerJobRestartPolicy);
    templateSpec.spec(podSpec);
    templateSpec.metadata(getMetadata(componentName, componentId, activityId, taskId, null));

    jobSpec.backoffLimit(kubeWorkerJobBackOffLimit);
    jobSpec.template(templateSpec);
    Integer ttl = ONE_DAY_IN_SECONDS * kubeWorkerJobTTLDays;
    LOGGER.info("Setting Job TTL at " + ttl + " seconds");
    jobSpec.setTtlSecondsAfterFinished(ttl);
    body.spec(jobSpec);

    return body;
  }

  protected V1ConfigMap createTaskConfigMapBody(String componentName, String componentId,
      String activityId, String taskName, String taskId, Map<String, String> inputProps) {
    V1ConfigMap body = new V1ConfigMap();
    body.metadata(getMetadata(componentName, componentId, activityId, taskId, PREFIX_CFGMAP));

    // Create Data
    Map<String, String> inputsWithFixedKeys = new HashMap<>();
    inputsWithFixedKeys.put("task.input.properties", createConfigMapProp(inputProps));
    body.data(inputsWithFixedKeys);
    return body;
  }

  protected V1ConfigMap createWorkflowConfigMapBody(String componentName, String componentId,
      String activityId, Map<String, String> inputProps) {
    V1ConfigMap body = new V1ConfigMap();
    body.metadata(getMetadata(componentName, componentId, activityId, null, PREFIX_CFGMAP));

    // Create Data
    Map<String, String> inputsWithFixedKeys = new HashMap<>();
    Map<String, String> sysProps = new HashMap<>();
    sysProps.put("activity.id", activityId);
    sysProps.put("workflow.name", componentName);
    sysProps.put("workflow.id", componentId);
    sysProps.put("worker.debug", kubeWorkerDebug.toString());
    sysProps.put("controller.service.url", bmrgControllerServiceURL);
    inputsWithFixedKeys.put("workflow.input.properties", createConfigMapProp(inputProps));
    inputsWithFixedKeys.put("workflow.system.properties", createConfigMapProp(sysProps));
    body.data(inputsWithFixedKeys);
    return body;
  }

  protected String getLabelSelector(String componentId, String activityId, String taskId) {
    StringBuilder labelSelector = new StringBuilder("platform=" + ORG + ",product=" + PRODUCT + ",tier=" + TIER);
    Optional.ofNullable(componentId).ifPresent(str -> labelSelector.append(",component-id=" + str));
    Optional.ofNullable(activityId).ifPresent(str -> labelSelector.append(",activity-id=" + str));
    Optional.ofNullable(taskId).ifPresent(str -> labelSelector.append(",task-id=" + str));

    LOGGER.info("  labelSelector: " + labelSelector.toString());
    return labelSelector.toString();
  }

  protected Map<String, String> createAnnotations(String componentName, String componentId,
      String activityId, String taskId) {
    Map<String, String> annotations = new HashMap<>();
    annotations.put("boomerangplatform.net/platform", ORG);
    annotations.put("boomerangplatform.net/product", PRODUCT);
    annotations.put("boomerangplatform.net/tier", TIER);
    annotations.put("boomerangplatform.net/component-name", componentName);
    Optional.ofNullable(componentId)
    .ifPresent(str -> annotations.put("boomerangplatform.net/component-id", componentId));
    Optional.ofNullable(activityId)
    .ifPresent(str -> annotations.put("boomerangplatform.net/activity-id", activityId));
    Optional.ofNullable(taskId)
        .ifPresent(str -> annotations.put("boomerangplatform.net/task-id", str));

    return annotations;
  }

  protected Map<String, String> createLabels(String componentId, String activityId, String taskId) {
    Map<String, String> labels = new HashMap<>();
    labels.put("platform", ORG);
    labels.put("product", PRODUCT);
    labels.put("tier", TIER);
    Optional.ofNullable(componentId).ifPresent(str -> labels.put("component-id", str));
    Optional.ofNullable(activityId).ifPresent(str -> labels.put("activity-id", str));
    Optional.ofNullable(taskId).ifPresent(str -> labels.put("task-id", str));
    return labels;
  }
}
