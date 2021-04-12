package net.boomerangplatform.kube.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.ConfigMapBuilder;
import io.fabric8.kubernetes.api.model.EnvVar;
import io.fabric8.kubernetes.api.model.PersistentVolumeClaim;
import io.fabric8.kubernetes.api.model.PersistentVolumeClaimBuilder;
import io.fabric8.kubernetes.api.model.PersistentVolumeClaimList;
import io.fabric8.kubernetes.api.model.PodSpec;
import io.fabric8.kubernetes.api.model.PodSpecBuilder;
import io.fabric8.kubernetes.api.model.Quantity;
import io.fabric8.kubernetes.api.model.ResourceRequirements;
import io.fabric8.kubernetes.api.model.SecurityContext;
import io.fabric8.kubernetes.api.model.batch.Job;
import io.fabric8.kubernetes.api.model.batch.JobBuilder;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientException;
import io.fabric8.kubernetes.client.Watch;
import io.fabric8.kubernetes.client.Watcher;
import io.fabric8.kubernetes.client.WatcherException;
import net.boomerangplatform.model.TaskConfiguration;

@Component
public class NewKubeServiceImpl {

  private static final Logger LOGGER = LogManager.getLogger(NewKubeServiceImpl.class);

  @Autowired
  protected NewHelperKubeServiceImpl helperKubeService;

  @Value("${kube.namespace}")
  protected String kubeNamespace;
  
  @Value("${kube.image.pullPolicy}")
  protected String kubeImagePullPolicy;

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

  @Value("${kube.worker.debug}")
  private Boolean taskEnableDebug;

  @Value("${controller.service.host}")
  protected String controllerServiceURL;

  KubernetesClient client = null;

  public NewKubeServiceImpl() {
    this.client = new DefaultKubernetesClient();
  }

  public boolean checkWorkspacePVCExists(String workspaceId, boolean failIfNotBound) {
    return workspaceId != null
        ? checkPVCExists(helperKubeService.getWorkspaceLabels(workspaceId, null), failIfNotBound)
        : false;
  }

  public boolean checkWorkflowPVCExists(String workflowId, String workflowActivityId, String taskId,
      String taskActivityId, boolean failIfNotBound) {
    return checkPVCExists(helperKubeService.getWorkflowLabels(workflowId, workflowActivityId, null),
        failIfNotBound);
  }

  private boolean checkPVCExists(Map<String, String> labelSelector, boolean failIfNotBound) {
    boolean pvcExists = false;
    try {
      PersistentVolumeClaimList pvcList =
          client.persistentVolumeClaims().withLabels(labelSelector).list();

      LOGGER.info("PVC List: " + pvcList);

      pvcExists = isPVCAvailable(failIfNotBound, pvcList);

      LOGGER.info("Is PVC Available: " + pvcExists);
    } catch (KubernetesClientException e) {
      LOGGER.error("No PVC found matching selector: " + labelSelector, e);
      return false;
    }
    return pvcExists;
  }

  public PersistentVolumeClaim createWorkspacePVC(String workspaceName, String workspaceId,
      Map<String, String> customLabels, String size, String className, String accessMode,
      long waitSeconds) throws KubernetesClientException, InterruptedException {
    return createPVC(helperKubeService.getWorkspaceAnnotations(workspaceName, workspaceId),
        helperKubeService.getWorkspaceLabels(workspaceId, customLabels), size, className,
        accessMode, waitSeconds);
  }

  public PersistentVolumeClaim createWorkflowPVC(String workflowName, String workflowId,
      String workflowActivityId, Map<String, String> customLabels, String size, String className,
      String accessMode, long waitSeconds) throws KubernetesClientException, InterruptedException {
    return createPVC(
        helperKubeService.getAnnotations("workflow", workflowName, workflowId, workflowActivityId,
            null, null),
        helperKubeService.getWorkflowLabels(workflowId, workflowActivityId, customLabels), size,
        className, accessMode, waitSeconds);
  }

  private PersistentVolumeClaim createPVC(Map<String, String> annotations,
      Map<String, String> labels, String size, String className, String accessMode,
      long waitSeconds) throws KubernetesClientException, InterruptedException {

    LOGGER.debug("Creating PersistentVolumeClaim object");

    PersistentVolumeClaim persistentVolumeClaim = new PersistentVolumeClaimBuilder()
        .withNewMetadata().withGenerateName(helperKubeService.getPrefixPVC() + "-")
        .withLabels(labels).withAnnotations(annotations).endMetadata().withNewSpec()
        .withStorageClassName(className).withAccessModes(accessMode).withNewResources()
        .addToRequests("storage", new Quantity(size)).endResources().endSpec().build();

    PersistentVolumeClaim result = client.persistentVolumeClaims().create(persistentVolumeClaim);

    client.resource(result).waitUntilCondition(
        r -> "Bound".equals(r.getStatus().getPhase()) || "Pending".equals(r.getStatus().getPhase()),
        30, TimeUnit.SECONDS);

    // client.resource(result).waitUntilCondition(r -> "Bound".equals(r.getStatus().getPhase()),
    // waitSeconds, TimeUnit.SECONDS);


    LOGGER.info(result);
    return result;
  }

  private boolean isPVCAvailable(boolean failIfNotBound,
      PersistentVolumeClaimList persistentVolumeClaimList) {
    if (!persistentVolumeClaimList.getItems().isEmpty()) {
      persistentVolumeClaimList.getItems().forEach(pvc -> LOGGER
          .info("PVC: " + pvc.getMetadata().getName() + " (" + pvc.getStatus().getPhase() + ")"));
      if (failIfNotBound) {
        if (persistentVolumeClaimList.getItems().stream()
            .filter(pvc -> "Bound".equalsIgnoreCase(pvc.getStatus().getPhase())).count() > 0) {
          // TODO update to check if they are terminating (even though they are still bound)
          return true;
        }
      } else {
        return true;
      }
    }

    return false;
  }

  public void deleteWorkspacePVC(String workspaceId) {
    deletePVC(helperKubeService.getWorkspaceLabels(workspaceId, null));
  }

  public void deleteWorkflowPVC(String workflowId, String workflowActivityId) {
    deletePVC(helperKubeService.getWorkflowLabels(workflowId, workflowActivityId, null));
  }

  private void deletePVC(Map<String, String> labels) {

    LOGGER.debug("Deleting PersistentVolumeClaim...");

    LOGGER.debug(client.persistentVolumeClaims().list().toString());

    client.persistentVolumeClaims().withLabels(labels).delete();
    // V1DeleteOptions deleteOptions = new V1DeleteOptions();
    // // deleteOptions.setPropagationPolicy("Background");
    // V1Status result = new V1Status();
    // String pvcName = getPVCName(labelSelector);
    // if (!pvcName.isEmpty()) {
    // LOGGER.info("Deleting PVC (" + pvcName + ")...");
    // try {
    // result = getCoreApi().deleteNamespacedPersistentVolumeClaim(pvcName, kubeNamespace,
    // kubeApiPretty, deleteOptions, null, null, null, null);
    // } catch (JsonSyntaxException e) {
    // if (e.getCause() instanceof IllegalStateException) {
    // IllegalStateException ise = (IllegalStateException) e.getCause();
    // if (ise.getMessage() != null
    // && ise.getMessage().contains("Expected a string but was BEGIN_OBJECT")) {
    // LOGGER.error(
    // "Catching exception because of issue https://github.com/kubernetes-client/java/issues/86");
    // } else {
    // LOGGER.error("Exception when running deletePVC()", e);
    // }
    // }
    // } catch (ApiException e) {
    // LOGGER.error("Exception when running deletePVC()", e);
    // }
    // }
  }

  public ConfigMap createWorkflowConfigMap(String workflowName, String workflowId,
      String workflowActivityId, Map<String, String> customLabels, Map<String, String> inputProps) {
    
  Map<String, String> dataMap = new HashMap<>();
  Map<String, String> sysProps = new HashMap<>();
  sysProps.put("controller-service-url", controllerServiceURL);
  sysProps.put("workflow-name", workflowName);
  sysProps.put("workflow-id", workflowId);
  sysProps.put("workflow-activity-id", workflowActivityId);
  dataMap.put("workflow.input.properties",
      helperKubeService.createConfigMapProp(inputProps));
  dataMap.put("workflow.system.properties",
      helperKubeService.createConfigMapProp(sysProps));
    
//  try (Watch ignored = watchWorkflowConfigMap(client,
//      helperKubeService.getWorkflowLabels(workflowId, workflowActivityId, customLabels));) {
    ConfigMap configMap = new ConfigMapBuilder().withNewMetadata()
        .withGenerateName(helperKubeService.getPrefixCM() + "-")
        .withLabels(
            helperKubeService.getWorkflowLabels(workflowId, workflowActivityId, customLabels))
        .withAnnotations(helperKubeService.getAnnotations("workflow", workflowName, workflowId,
            workflowActivityId, null, null))
        .endMetadata().addToData(dataMap).build();
    ConfigMap result = client.configMaps().create(configMap);
//  }
  
    return result;
  }
  
//  private static Watch watchWorkflowConfigMap(KubernetesClient client, Map<String, String> labels) {
//    
//    return client.configMaps().withLabels(labels).watch(new Watcher<ConfigMap>() {
//      @Override
//      public void eventReceived(Action action, ConfigMap resource) {
//        LOGGER.info("Watch event received {}: {}", action.name(), resource.getMetadata().getName());
//        switch (action.name()) {
//          case "DELETED":
//            LOGGER.info(resource.getMetadata().getName() + "got deleted");
//              break;
//        }
//      }
//
//      @Override
//      public void onClose(WatcherException e) {
//        LOGGER.error("Watch error received: {}", e.getMessage(), e);
//        //Cause the pod to restart
//        System.exit(1);
//      }
//
//      @Override
//      public void onClose() {
//        LOGGER.info("Watch gracefully closed");
//      }
//    });
//  }
  
public ConfigMap createTaskConfigMap(String workflowName, String workflowId,
    String workflowActivityId, String taskName, String taskId, String taskActivityId,
    Map<String, String> customLabels, Map<String, String> inputProps) {

  Map<String, String> dataMap = new HashMap<>();
  Map<String, String> sysProps = new HashMap<>();
  sysProps.put("task-id", taskId);
  sysProps.put("task-name", taskName);
  sysProps.put("task-activity-id", taskActivityId);
  sysProps.put("controller-service-url", controllerServiceURL);
  sysProps.put("workflow-name", workflowName);
  sysProps.put("workflow-id", workflowId);
  sysProps.put("workflow-activity-id", workflowActivityId);
  dataMap.put("task.input.properties", helperKubeService.createConfigMapProp(inputProps));
  dataMap.put("task.system.properties", helperKubeService.createConfigMapProp(sysProps));

  ConfigMap configMap = new ConfigMapBuilder().withNewMetadata()
      .withGenerateName(helperKubeService.getPrefixCM() + "-")
      .withLabels(helperKubeService.getTaskLabels(workflowId, workflowActivityId, taskId, taskActivityId, customLabels))
      .withAnnotations(helperKubeService.getAnnotations("task", workflowName, workflowId,
          workflowActivityId, taskId, taskActivityId))
      .endMetadata().addToData(dataMap).build();
  ConfigMap result = client.configMaps().create(configMap);

  return result;
}

  // protected V1ConfigMap createTaskConfigMapBodyFromEnv(String workflowName, String workflowId,
  // String workflowActivityId, String taskName, String taskId, Map<String, String> parameters) {
  // V1ConfigMap body = new V1ConfigMap();
  //
  // body.metadata(
  // helperKubeService.getMetadata(workflowName, workflowId, workflowActivityId, taskId,
  // helperKubeService.getPrefixCFGMAP()));
  //
  //// Create Data
  // Map<String, String> envParameters = new HashMap<>();
  // envParameters.put("SYSTEM_ACTIVITY_ID", workflowActivityId);
  // envParameters.put("SYSTEM_WORKFLOW_NAME", workflowName);
  // envParameters.put("SYSTEM_WORKFLOW_ID", workflowId);
  // envParameters.put("SYSTEM_CONTROLLER_URL", bmrgControllerServiceURL);
  //
  // for (Map.Entry<String, String> entry : parameters.entrySet()) {
  // envParameters.put(entry.getKey().replace("-", "").replace(" ", "").replace(".",
  // "_").toUpperCase(), entry.getValue());
  // }
  //
  // body.data(envParameters);
  // return body;
  // }

  public void deleteWorkflowConfigMap(String workflowId, String workflowActivityId) {
    deleteConfigMap(helperKubeService.getWorkflowLabels(workflowId, workflowActivityId, null));
  }
  
  private void deleteConfigMap(Map<String, String> labels) {

    LOGGER.debug("Deleting ConfigMap...");

    LOGGER.debug(client.configMaps().list().toString());

    client.configMaps().withLabels(labels).delete();
  }
  
  private Job createJob(boolean createLifecycleWatcher, String workspaceId, String workflowName,
      String workflowId, String workflowActivityId, String taskActivityId, String taskName,
      String taskId, Map<String, String> customLabels, List<String> arguments,
      Map<String, String> taskProperties, String image, String command,
      TaskConfiguration taskConfiguration) {

    LOGGER.info("Initializing Job...");
    
    /*
     * Define environment variables made up of
     * TODO: securityContext.setProcMount("Unmasked");
     *  - Only works with Kube 1.12. ICP 3.1.1 is Kube 1.11.5
     * TODO: determine if we need to do no network as an option
     */
    SecurityContext securityContext = new SecurityContext();
    securityContext.setPrivileged(true);
    
    /*
     * Define environment variables made up of
     * - Proxy (if enabled)
     * - Boomerang Flow env vars
     * - Debug and CI
     */
    List<EnvVar> envVars = new ArrayList<>();
    envVars.addAll(helperKubeService.createProxyEnvVars());
    envVars.addAll(helperKubeService.createEnvVars(workflowId, workflowActivityId, taskName, taskId, taskActivityId));
    envVars.add(helperKubeService.createEnvVar("DEBUG", taskEnableDebug.toString()));
    envVars.add(helperKubeService.createEnvVar("CI", "true"));
    
    /*
     * Create a resource request and limit for ephemeral-storage Defaults to application.properties,
     * can be overridden by user property.
     * Create a resource request and limit for memory Defaults to application.properties, can be
     * overridden by user property. Maximum of 32Gi.
     */
    ResourceRequirements resources = new ResourceRequirements();
    Map<String, Quantity> resourceRequests = new HashMap<>();
    resourceRequests.put("ephemeral-storage", new Quantity(kubeResourceRequestEphemeralStorage));
    resourceRequests.put("memory", new Quantity(kubeResourceRequestMemory));
    Map<String, Quantity> resourceLimits = new HashMap<>();
    resourceLimits.put("ephemeral-storage", new Quantity(kubeResourceLimitEphemeralStorage));
    String kubeResourceLimitMemoryQuantity = taskProperties.get("worker.resource.memory.size");
    if (kubeResourceLimitMemoryQuantity != null && !(Integer.valueOf(kubeResourceLimitMemoryQuantity.replace("Gi", "")) > 32)) {
      LOGGER.info("Setting Resource Memory Limit to " + kubeResourceLimitMemoryQuantity + "...");
      resourceLimits.put("memory", new Quantity(kubeResourceLimitMemoryQuantity));
    } else {
      LOGGER
          .info("Setting Resource Memory Limit to default of: " + kubeResourceLimitMemory + " ...");
      resourceLimits.put("memory", new Quantity(kubeResourceLimitMemory));
    }
    resources.setLimits(resourceLimits);
    
    /*
     * Job Specification builder
     */
    Job job = new JobBuilder()
        .withApiVersion("batch/v1")
        .withNewMetadata()
        .withGenerateName(helperKubeService.getPrefixTask() + "-")
        .withLabels(helperKubeService.getTaskLabels(workflowId, workflowActivityId, taskId, taskActivityId, customLabels))
        .withAnnotations(helperKubeService.getAnnotations("task", workflowName, workflowId,
            workflowActivityId, taskId, taskActivityId))
        .endMetadata()
        .withNewSpec()
        .withNewTemplate()
        .withNewSpec()
        .addNewContainer()
        .withName("task-cntr")
        .withImage(image)
        .withCommand(command != null && !command.isEmpty() ? command : "")
        .withArgs(arguments)
        .withImagePullPolicy(kubeImagePullPolicy)
        .withSecurityContext(securityContext)
        .withEnv(envVars)
        .withResources(resources)
        .endContainer()
        .withRestartPolicy("Never")
        .endSpec()
        .endTemplate()
        .endSpec()
        .build();

    /*
     * Create volumes 
     * - /workspace for cross workflow persistence such as caches (optional if mounted prior) 
     * - /workflow for workflow based sharing between tasks (optional if mounted prior) 
     * - /props for mounting config_maps 
     * - /data for task storage (optional - needed if using in memory storage)
     */
    if (checkWorkspacePVCExists(workspaceId, true)) {
      container.addVolumeMountsItem(
          getVolumeMount(helperKubeService.getPrefixVol() + "-ws", "/workspace"));
      V1Volume workspaceVolume = getVolume(helperKubeService.getPrefixVol() + "-ws");
      V1PersistentVolumeClaimVolumeSource workerVolumePVCSource =
          new V1PersistentVolumeClaimVolumeSource();
      workspaceVolume.persistentVolumeClaim(workerVolumePVCSource
          .claimName(getPVCName(helperKubeService.getWorkspaceLabelSelector(workspaceId))));
      podSpec.addVolumesItem(workspaceVolume);
    }

    if (!getPVCName(
        helperKubeService.getLabelSelector("workflow", workflowId, workflowActivityId, null, null))
            .isEmpty()) {
      container.addVolumeMountsItem(
          getVolumeMount(helperKubeService.getPrefixVol() + "-wf", "/workflow"));
      V1Volume workerVolume = getVolume(helperKubeService.getPrefixVol() + "-wf");
      V1PersistentVolumeClaimVolumeSource workerVolumePVCSource =
          new V1PersistentVolumeClaimVolumeSource();
      workerVolume
          .persistentVolumeClaim(workerVolumePVCSource.claimName(getPVCName(helperKubeService
              .getLabelSelector("workflow", workflowId, workflowActivityId, null, null))));
      podSpec.addVolumesItem(workerVolume);
    }

    /*
     * The following code is integrated to the helm chart and CICD properties It allows for
     * containers that breach the standard ephemeral-storage size by off-loading to memory See:
     * https://kubernetes.io/docs/concepts/storage/volumes/#emptydir
     */
    container
        .addVolumeMountsItem(getVolumeMount(helperKubeService.getPrefixVol() + "-data", "/data"));
    V1Volume dataVolume = getVolume(helperKubeService.getPrefixVol() + "-data");
    V1EmptyDirVolumeSource emptyDir = new V1EmptyDirVolumeSource();
    if (kubeWorkerStorageDataMemory
        && Boolean.valueOf(taskProperties.get("worker.storage.data.memory"))) {
      LOGGER.info("Setting /data to in memory storage...");
      emptyDir.setMedium("Memory");
    }
    dataVolume.emptyDir(emptyDir);
    podSpec.addVolumesItem(dataVolume);

    container
        .addVolumeMountsItem(getVolumeMount(helperKubeService.getPrefixVol() + "-props", "/props"));

    // Creation of Projected Volume with multiple ConfigMaps
    V1Volume volumeProps = getVolume(helperKubeService.getPrefixVol() + "-props");
    V1ProjectedVolumeSource projectedVolPropsSource = new V1ProjectedVolumeSource();
    List<V1VolumeProjection> projectPropsVolumeList = new ArrayList<>();

    // Add Workflow Configmap Projected Volume
    V1ConfigMap wfConfigMap = getConfigMap(
        helperKubeService.getLabelSelector("workflow", workflowId, workflowActivityId, null, null));
    if (wfConfigMap != null && !getConfigMapName(wfConfigMap).isEmpty()) {
      projectPropsVolumeList.add(getVolumeProjection(wfConfigMap));
    }

    // Add Task Configmap Projected Volume
    V1ConfigMap taskConfigMap = getConfigMap(helperKubeService.getLabelSelector("task", workflowId,
        workflowActivityId, taskId, taskActivityId));
    if (taskConfigMap != null && !getConfigMapName(taskConfigMap).isEmpty()) {
      projectPropsVolumeList.add(getVolumeProjection(taskConfigMap));
    }

    // Add all configmap projected volume
    projectedVolPropsSource.sources(projectPropsVolumeList);
    volumeProps.projected(projectedVolPropsSource);
    podSpec.addVolumesItem(volumeProps);

    // V1ConfigMap taskConfigMap = getConfigMap(null, workflowActivityId, taskId);
    // V1EnvFromSource envAsProps = new V1EnvFromSource();
    // V1ConfigMapEnvSource envCMRef = new V1ConfigMapEnvSource();
    // envCMRef.setName(getConfigMapName(taskConfigMap));
    // envAsProps.setConfigMapRef(envCMRef);
    // envAsProps.setPrefix("PARAMS_");
    //
    // container.addEnvFromItem(envAsProps);

    /*
     * The following code is for tasks with Lifecycle enabled.
     */
    if (createLifecycleWatcher) {
      List<V1Container> initContainers = new ArrayList<>();
      V1Container initContainer = getContainer(kubeLifecycleImage, null).name("init-cntr")
          .addVolumeMountsItem(getVolumeMount("lifecycle", "/lifecycle")).addArgsItem("lifecycle")
          .addArgsItem("init");
      initContainers.add(initContainer);
      podSpec.setInitContainers(initContainers);
      V1Container lifecycleContainer = getContainer(kubeLifecycleImage, null).name("lifecycle-cntr")
          .addVolumeMountsItem(getVolumeMount("lifecycle", "/lifecycle"))
          .addVolumeMountsItem(
              getVolumeMount(helperKubeService.getPrefixVol() + "-props", "/props"))
          .addArgsItem("lifecycle").addArgsItem("wait");
      lifecycleContainer.env(helperKubeService.createEnvVars(workflowId, workflowActivityId,
          taskName, taskId, taskActivityId));
      lifecycleContainer.addEnvItem(helperKubeService.createEnvVar("DEBUG",
          helperKubeService.getTaskDebug(taskConfiguration)));
      containerList.add(lifecycleContainer);
      container.addVolumeMountsItem(getVolumeMount("lifecycle", "/lifecycle"));
      V1Volume lifecycleVol = getVolume("lifecycle");
      V1EmptyDirVolumeSource emptyDir2 = new V1EmptyDirVolumeSource();
      lifecycleVol.emptyDir(emptyDir2);
      podSpec.addVolumesItem(lifecycleVol);
    }

    if (kubeJobDedicatedNodes) {
      helperKubeService.getTolerationAndSelector(podSpec);
    }

    helperKubeService.getPodAntiAffinity(podSpec,
        helperKubeService.createAntiAffinityLabels("task"));

    if (!kubeJobHostAliases.isEmpty()) {
      Type listHostAliasType = new TypeToken<List<V1HostAlias>>() {}.getType();
      List<V1HostAlias> hostAliasList = new Gson().fromJson(kubeJobHostAliases, listHostAliasType);
      podSpec.hostAliases(hostAliasList);
    }

    if (!kubeJobServiceAccount.isEmpty()) {
      podSpec.serviceAccountName(kubeJobServiceAccount);
    }

    containerList.add(container);
    podSpec.containers(containerList);
    V1LocalObjectReference imagePullSecret = new V1LocalObjectReference();
    imagePullSecret.name(kubeImagePullSecret);
    List<V1LocalObjectReference> imagePullSecretList = new ArrayList<>();
    imagePullSecretList.add(imagePullSecret);
    podSpec.imagePullSecrets(imagePullSecretList);
    podSpec.restartPolicy(kubeJobRestartPolicy);
    templateSpec.spec(podSpec);
    templateSpec.metadata(helperKubeService.getMetadata("task", workflowName, workflowId,
        workflowActivityId, taskId, taskActivityId, null, customLabels));

    jobSpec.backoffLimit(kubeJobBackOffLimit);
    jobSpec.template(templateSpec);
    Integer ttl = ONE_DAY_IN_SECONDS * kubeJobTTLDays;
    LOGGER.info("Setting Job TTL at " + ttl + " seconds");
    jobSpec.setTtlSecondsAfterFinished(ttl);
    body.spec(jobSpec);

    return body;
  }
}
