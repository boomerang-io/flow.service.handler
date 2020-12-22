package net.boomerangplatform.kube.service;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
import net.boomerangplatform.model.TaskConfiguration;

@Service
@Profile("cicd")
public class CICDKubeServiceImpl extends AbstractKubeServiceImpl {

  private static final Logger LOGGER = LogManager.getLogger(CICDKubeServiceImpl.class);

  @Value("${kube.api.timeout}")
  private Integer kubeApiTimeOut;
  
  @Value("${kube.resource.limit.ephemeral-storage}")
  private String kubeResourceLimitEphemeralStorage;
  
  @Value("${kube.resource.request.ephemeral-storage}")
  private String kubeResourceRequestEphemeralStorage;
  
  @Value("${kube.resource.limit.memory}")
  private String kubeResourceLimitMemory;
  
  @Value("${kube.resource.request.memory}")
  private String kubeResourceRequestMemory;
  
  @Value("${kube.worker.storage.data.memory}")
  private Boolean kubeWorkerStorageDataMemory;

  /**
 *
 */
@Override
  protected V1Job createJobBody(boolean createLifecycle, String workflowName, String workflowId, String workflowActivityId, String taskActivityId,
      String taskName, String taskId, List<String> arguments,
      Map<String, String> taskProperties, String image, String command, TaskConfiguration taskConfiguration) {

    // Initialize Job Body
    V1Job body = new V1Job();
    V1ObjectMeta jobMetadata = helperKubeService.getMetadata(workflowName, workflowId, workflowActivityId, taskId, null);
	jobMetadata.name(helperKubeService.getPrefixJob() + "-" + workflowActivityId);
    body.metadata(jobMetadata);

    // Create Spec
    V1JobSpec jobSpec = new V1JobSpec();
    V1PodTemplateSpec templateSpec = new V1PodTemplateSpec();
    V1PodSpec podSpec = new V1PodSpec();
    V1Container container = getContainer(image, command);
    List<V1EnvVar> envVars = new ArrayList<>();
    if (proxyEnabled) {
      envVars.addAll(helperKubeService.createProxyEnvVars());
    }
    envVars.add(helperKubeService.createEnvVar("DEBUG", helperKubeService.getTaskDebug(taskConfiguration)));
    envVars.add(helperKubeService.createEnvVar("CI", "true"));
    container.env(envVars);
    container.args(arguments);
    V1ResourceRequirements resources = new V1ResourceRequirements();
    resources.putRequestsItem("ephemeral-storage", new Quantity(kubeResourceRequestEphemeralStorage));
    resources.putLimitsItem("ephemeral-storage", new Quantity(kubeResourceLimitEphemeralStorage));
    /*
     * Create a resource request and limit for memory
     * Defaults to application.properties, can be overridden by user property. Maximum of 32Gi.
     */
	resources.putRequestsItem("memory", new Quantity(kubeResourceRequestMemory));
	String kubeResourceLimitMemoryProp = taskProperties.get("worker.resource.memory.size");
	if (kubeResourceLimitMemoryProp != null && !(Integer.valueOf(kubeResourceLimitMemoryProp.replace("Gi", "")) > 32)) {
		LOGGER.info("Setting Resource Memory Limit to " + kubeResourceLimitMemoryProp + "...");
	    resources.putLimitsItem("memory", new Quantity(kubeResourceLimitMemoryProp));
	} else {
		LOGGER.info("Setting Resource Memory Limit to default of: " + kubeResourceLimitMemory + " ...");
	    resources.putLimitsItem("memory", new Quantity(kubeResourceLimitMemory));
	}
    container.setResources(resources);
    if (checkWorkspacePVCExists(workflowId, true)) {
      container.addVolumeMountsItem(getVolumeMount(helperKubeService.getPrefixVolCache(), "/cache"));
      V1Volume workspaceVolume = getVolume(helperKubeService.getPrefixVolCache());
      V1PersistentVolumeClaimVolumeSource workerVolumePVCSource =
          new V1PersistentVolumeClaimVolumeSource();
      workspaceVolume
          .persistentVolumeClaim(workerVolumePVCSource.claimName(getPVCName(helperKubeService.getWorkspaceLabelSelector(workflowId))));
      podSpec.addVolumesItem(workspaceVolume);
    }
    container.addVolumeMountsItem(getVolumeMount(helperKubeService.getPrefixVolProps(), "/props"));

    /*  The following code is integrated to the helm chart and CICD properties
    * 	It allows for containers that breach the standard ephemeral-storage size by off-loading to memory
    * 	See: https://kubernetes.io/docs/concepts/storage/volumes/#emptydir
    */
    container.addVolumeMountsItem(getVolumeMount(helperKubeService.getPrefixVolData(), "/data"));
	V1Volume dataVolume = getVolume(helperKubeService.getPrefixVolData());
	V1EmptyDirVolumeSource emptyDir = new V1EmptyDirVolumeSource();
    if (kubeWorkerStorageDataMemory && Boolean.valueOf(taskProperties.get("worker.storage.data.memory"))) {
    	LOGGER.info("Setting /data to in memory storage...");
    	emptyDir.setMedium("Memory");
    }
	dataVolume.emptyDir(emptyDir);
	podSpec.addVolumesItem(dataVolume);

    // Creation of Projected Volume for multiple ConfigMaps
    V1Volume volumeProps = getVolume(helperKubeService.getPrefixVolProps());
    V1ProjectedVolumeSource projectedVolPropsSource = new V1ProjectedVolumeSource();
    List<V1VolumeProjection> projectPropsVolumeList = new ArrayList<>();

    // Add Worfklow Configmap Projected Volume
    V1ConfigMap wfConfigMap = getConfigMap(workflowId, workflowActivityId, null);
    if (wfConfigMap != null && !getConfigMapName(wfConfigMap).isEmpty()) {
      projectPropsVolumeList.add(getVolumeProjection(wfConfigMap));
    }
    // Add Task Configmap Projected Volume
    V1ConfigMap taskConfigMap = getConfigMap(workflowId, workflowActivityId, taskId);
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
      helperKubeService.getTolerationAndSelector(podSpec);
    }

    helperKubeService.getPodAntiAffinity(podSpec, helperKubeService.createAntiAffinityLabels());

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
    templateSpec.metadata(helperKubeService.getMetadata(workflowName, workflowId, workflowActivityId, taskId, null));

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
    body.metadata(helperKubeService.getMetadata(componentName, componentId, activityId, taskId, helperKubeService.getPrefixCFGMAP()));

    // Create Data
    Map<String, String> inputsWithFixedKeys = new HashMap<>();
    inputsWithFixedKeys.put("task.input.properties", helperKubeService.createConfigMapProp(inputProps));
    body.data(inputsWithFixedKeys);
    return body;
  }

  protected V1ConfigMap createWorkflowConfigMapBody(String componentName, String componentId,
      String activityId, Map<String, String> inputProps) {
    V1ConfigMap body = new V1ConfigMap();
    body.metadata(helperKubeService.getMetadata(componentName, componentId, activityId, null, helperKubeService.getPrefixCFGMAP()));

    // Create Data
    Map<String, String> inputsWithFixedKeys = new HashMap<>();
    Map<String, String> sysProps = new HashMap<>();
    sysProps.put("activity.id", activityId);
    sysProps.put("workflow.name", componentName);
    sysProps.put("workflow.id", componentId);
    sysProps.put("controller.service.url", bmrgControllerServiceURL);
    inputsWithFixedKeys.put("workflow.input.properties", helperKubeService.createConfigMapProp(inputProps));
    inputsWithFixedKeys.put("workflow.system.properties", helperKubeService.createConfigMapProp(sysProps));
    body.data(inputsWithFixedKeys);
    return body;
  }
}
