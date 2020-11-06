package net.boomerangplatform.kube.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import io.kubernetes.client.models.V1ConfigMap;
import io.kubernetes.client.models.V1Container;
import io.kubernetes.client.models.V1EmptyDirVolumeSource;
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
import net.boomerangplatform.model.TaskConfiguration;

@Service
@Profile({"live", "local"})
public class FlowKubeServiceImpl extends AbstractKubeServiceImpl {

  private static final Logger LOGGER = LogManager.getLogger(FlowKubeServiceImpl.class);

  @Value("${kube.lifecycle.image}")
  private String kubeLifecycleImage;

  @Value("${kube.api.timeout}")
  private Integer kubeApiTimeOut;

  @Override
  protected V1Job createJobBody(boolean createLifecycle, String workflowName, String workflowId, String workflowActivityId, String taskActivityId,
      String taskName, String taskId, List<String> arguments,
      Map<String, String> taskProperties, String image, String command, TaskConfiguration taskConfiguration) {

    // Initialize Job Body
    V1Job body = new V1Job();
    body.metadata(
        getMetadata(workflowName, workflowId, workflowActivityId, taskId, getPrefixJob() + "-" + taskActivityId));

    // Create Spec
    V1JobSpec jobSpec = new V1JobSpec();
    V1PodTemplateSpec templateSpec = new V1PodTemplateSpec();
    V1PodSpec podSpec = new V1PodSpec();
    V1Container container = getContainer(image, command);
    List<V1Container> containerList = new ArrayList<>();

    List<V1EnvVar> envVars = new ArrayList<>();
    if (proxyEnabled) {
      envVars.addAll(createProxyEnvVars());
    }
    envVars.addAll(createEnvVars(workflowId,workflowActivityId,taskName,taskId));
    envVars.add(createEnvVar("DEBUG", getTaskDebug(taskConfiguration)));
    container.env(envVars);
    container.args(arguments);
    if (!getPVCName(getLabelSelector(workflowId, workflowActivityId, null)).isEmpty()) {
      container.addVolumeMountsItem(getVolumeMount(getPrefixVolData(), "/data"));
      V1Volume workerVolume = getVolume(getPrefixVolData());
      V1PersistentVolumeClaimVolumeSource workerVolumePVCSource =
          new V1PersistentVolumeClaimVolumeSource();
      workerVolume.persistentVolumeClaim(
          workerVolumePVCSource.claimName(getPVCName(getLabelSelector(workflowId, workflowActivityId, null))));
      podSpec.addVolumesItem(workerVolume);
    }
    
    /*
     * The following code is for custom tasks only
     */
    if (createLifecycle) {
      List<V1Container> initContainers = new ArrayList<>();
      V1Container initContainer =
          getContainer(kubeLifecycleImage, null)
              .name("init-cntr")
              .addVolumeMountsItem(getVolumeMount("lifecycle", "/lifecycle"))
              .addArgsItem("lifecycle")
              .addArgsItem("init");
      initContainers.add(initContainer);
      podSpec.setInitContainers(initContainers);
      V1Container lifecycleContainer =
          getContainer(kubeLifecycleImage, null)
              .name("lifecycle-cntr")
              .addVolumeMountsItem(getVolumeMount("lifecycle", "/lifecycle"))
              .addArgsItem("lifecycle")
              .addArgsItem("wait");
    	lifecycleContainer.env(createEnvVars(workflowId,workflowActivityId,taskName,taskId));
    	containerList.add(lifecycleContainer);
    	container.addVolumeMountsItem(getVolumeMount("lifecycle", "/lifecycle"));
        V1Volume lifecycleVol = getVolume("lifecycle");
        V1EmptyDirVolumeSource emptyDir = new V1EmptyDirVolumeSource();
        lifecycleVol.emptyDir(emptyDir);
        podSpec.addVolumesItem(lifecycleVol);
    }
    
    container.addVolumeMountsItem(getVolumeMount(getPrefixVolProps(), "/props"));

    // Creation of Projected Volume with multiple ConfigMaps
    V1Volume volumeProps = getVolume(getPrefixVolProps());
    V1ProjectedVolumeSource projectedVolPropsSource = new V1ProjectedVolumeSource();
    List<V1VolumeProjection> projectPropsVolumeList = new ArrayList<>();

    // Add Worfklow Configmap Projected Volume
    V1ConfigMap wfConfigMap = getConfigMap(workflowId, workflowActivityId, null);
    if (wfConfigMap != null && !getConfigMapName(wfConfigMap).isEmpty()) {
      projectPropsVolumeList.add(getVolumeProjection(wfConfigMap));
    }

    // Add Task Configmap Projected Volume
    V1ConfigMap taskConfigMap = getConfigMap(null, workflowActivityId, taskId);
    if (taskConfigMap != null && !getConfigMapName(taskConfigMap).isEmpty()) {
      projectPropsVolumeList.add(getVolumeProjection(taskConfigMap));
    }

    // Add all configmap projected volume
    projectedVolPropsSource.sources(projectPropsVolumeList);
    volumeProps.projected(projectedVolPropsSource);
    podSpec.addVolumesItem(volumeProps);
    
    if (kubeWorkerDedicatedNodes) {
    	getTolerationAndSelector(podSpec);
    }

    getPodAntiAffinity(podSpec, createAntiAffinityLabels());

    if (!kubeWorkerServiceAccount.isEmpty()) {
      podSpec.serviceAccountName(kubeWorkerServiceAccount);
    }

    containerList.add(container);
    podSpec.containers(containerList);
    V1LocalObjectReference imagePullSecret = new V1LocalObjectReference();
    imagePullSecret.name(kubeImagePullSecret);
    List<V1LocalObjectReference> imagePullSecretList = new ArrayList<>();
    imagePullSecretList.add(imagePullSecret);
    podSpec.imagePullSecrets(imagePullSecretList);
    podSpec.restartPolicy(kubeWorkerJobRestartPolicy);
    templateSpec.spec(podSpec);
    templateSpec.metadata(getMetadata(workflowName, workflowId, workflowActivityId, taskId, null));

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
        getMetadata(workflowName, workflowId, workflowActivityId, taskId, getPrefixCFGMAP()));

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
        getMetadata(workflowName, workflowId, workflowActivityId, null, getPrefixCFGMAP()));

    // Create Data
    Map<String, String> inputsWithFixedKeys = new HashMap<>();
    Map<String, String> sysProps = new HashMap<>();
    sysProps.put("activity.id", workflowActivityId);
    sysProps.put("workflow.name", workflowName);
    sysProps.put("workflow.id", workflowId);
    sysProps.put("controller.service.url", bmrgControllerServiceURL);
    inputsWithFixedKeys.put("workflow.input.properties", createConfigMapProp(inputProps));
    inputsWithFixedKeys.put("workflow.system.properties", createConfigMapProp(sysProps));
    body.data(inputsWithFixedKeys);
    return body;
  }
  
}
