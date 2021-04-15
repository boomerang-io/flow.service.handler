package net.boomerangplatform.kube.service;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.ConfigMapBuilder;
import io.fabric8.kubernetes.api.model.ConfigMapList;
import io.fabric8.kubernetes.api.model.ConfigMapProjection;
import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.DeletionPropagation;
import io.fabric8.kubernetes.api.model.EmptyDirVolumeSource;
import io.fabric8.kubernetes.api.model.EnvVar;
import io.fabric8.kubernetes.api.model.HostAlias;
import io.fabric8.kubernetes.api.model.LocalObjectReference;
import io.fabric8.kubernetes.api.model.PersistentVolumeClaim;
import io.fabric8.kubernetes.api.model.PersistentVolumeClaimBuilder;
import io.fabric8.kubernetes.api.model.PersistentVolumeClaimList;
import io.fabric8.kubernetes.api.model.PersistentVolumeClaimVolumeSource;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.PodCondition;
import io.fabric8.kubernetes.api.model.ProjectedVolumeSource;
import io.fabric8.kubernetes.api.model.Quantity;
import io.fabric8.kubernetes.api.model.ResourceRequirements;
import io.fabric8.kubernetes.api.model.SecurityContext;
import io.fabric8.kubernetes.api.model.Toleration;
import io.fabric8.kubernetes.api.model.Volume;
import io.fabric8.kubernetes.api.model.VolumeMount;
import io.fabric8.kubernetes.api.model.VolumeProjection;
import io.fabric8.kubernetes.api.model.batch.Job;
import io.fabric8.kubernetes.api.model.batch.JobBuilder;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientException;
import io.fabric8.kubernetes.client.Watch;
import io.fabric8.kubernetes.client.Watcher;
import io.fabric8.kubernetes.client.WatcherException;
import net.boomerangplatform.model.TaskConfiguration;
import net.boomerangplatform.model.TaskDeletionEnum;

@Component
public class NewKubeServiceImpl {

  private static final Logger LOGGER = LogManager.getLogger(NewKubeServiceImpl.class);

  @Autowired
  protected NewHelperKubeServiceImpl helperKubeService;
    
  protected static final Integer ONE_DAY_IN_SECONDS = 86400; // 60*60*24

  @Value("${kube.namespace}")
  protected String kubeNamespace;
  
  @Value("${kube.image.pullPolicy}")
  protected String kubeImagePullPolicy;

  @Value("${kube.image.pullSecret}")
  protected String kubeImagePullSecret;

  @Value("${kube.worker.job.backOffLimit}")
  protected Integer kubeJobBackOffLimit;

  @Value("${kube.worker.job.restartPolicy}")
  protected String kubeJobRestartPolicy;
    
  @Value("${kube.worker.job.ttlDays}")
  protected Integer kubeJobTTLDays;

  @Value("${kube.worker.serviceaccount}")
  protected String kubeJobServiceAccount;

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
  
  @Value("${kube.lifecycle.image}")
  private String kubeLifecycleImage;

  @Value("${kube.worker.node.dedicated}")
  protected Boolean kubeJobDedicatedNodes;

  @Value("${kube.worker.hostaliases}")
  protected String kubeHostAliases;

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
  
  protected String getPVCName(Map<String, String> labels) {
    try {
      PersistentVolumeClaimList pvcList =
          client.persistentVolumeClaims().withLabels(labels).list();

      LOGGER.info("PVC List: " + pvcList.toString());
      
      if (!pvcList.getItems().isEmpty()) {
        LOGGER.info(" PVCs() - Found " + pvcList.getItems().size() + " persistentvolumeclaims: "
            + pvcList.getItems().stream().reduce("", (pvcNames, pvc) -> pvcNames +=
                pvc.getMetadata().getName() + "(" + pvc.getMetadata().getCreationTimestamp() + ")",
                String::concat));
        if (pvcList.getItems().get(0).getMetadata().getName() != null) {
          LOGGER.info(" Chosen PVC Name: " + pvcList.getItems().get(0).getMetadata().getName());
          return pvcList.getItems().get(0).getMetadata().getName();
        }
      }
    } catch (Exception e) {
      LOGGER.error(e);
    }
    return "";
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
        waitSeconds, TimeUnit.SECONDS);

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
  
//  Watch cmWatcher = watchWorkflowConfigMap(client, helperKubeService.getWorkflowLabels(workflowId, workflowActivityId, customLabels));
    
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

  public void deleteTaskConfigMap(String workflowId, String workflowActivityId, String taskId, String taskActivityId, Map<String, String> customLabels) {
    deleteConfigMap(helperKubeService.getTaskLabels(workflowId, workflowActivityId, taskId, taskActivityId, customLabels));
  }
  
  private void deleteConfigMap(Map<String, String> labels) {

    LOGGER.debug("Deleting ConfigMap...");

    LOGGER.debug(client.configMaps().list().toString());

    client.configMaps().withLabels(labels).delete();
  }
  
  protected String getConfigMapName(Map<String, String> labels) {
  try {
    ConfigMapList configMapList = client.configMaps().withLabels(labels).list();

    LOGGER.info("ConfigMap List: " + configMapList.toString());
    
    if (!configMapList.getItems().isEmpty()) {
      LOGGER.info(" ConfigMaps() - Found " + configMapList.getItems().size() + " persistentvolumeclaims: "
          + configMapList.getItems().stream().reduce("", (cmNames, cm) -> cmNames +=
              cm.getMetadata().getName() + "(" + cm.getMetadata().getCreationTimestamp() + ")",
              String::concat));
      if (configMapList.getItems().get(0).getMetadata().getName() != null) {
        LOGGER.info(" Chosen ConfigMap Name: " + configMapList.getItems().get(0).getMetadata().getName());
        return configMapList.getItems().get(0).getMetadata().getName();
      }
    }
  } catch (Exception e) {
    LOGGER.error(e);
  }
  return "";
  }
  
  public Job createJob(boolean createLifecycleWatcher, String workspaceId, String workflowName,
      String workflowId, String workflowActivityId, String taskActivityId, String taskName,
      String taskId, Map<String, String> customLabels, List<String> arguments,
      Map<String, String> taskProperties, String image, String command,
      TaskConfiguration taskConfiguration, long waitSeconds) throws InterruptedException {

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
     * Create volumes and Volume Mounts
     * - /workspace for cross workflow persistence such as caches (optional if mounted prior) 
     * - /workflow for workflow based sharing between tasks (optional if mounted prior) 
     * - /props for mounting config_maps 
     * - /data for task storage (optional - needed if using in memory storage)
     */
    List<VolumeMount> volumeMounts = new ArrayList<>();
    List<Volume> volumes = new ArrayList<>();
    if (checkWorkspacePVCExists(workspaceId, true)) {
      VolumeMount wsVolumeMount = new VolumeMount();
      wsVolumeMount.setName(helperKubeService.getPrefixVol() + "-ws");
      wsVolumeMount.setMountPath("/workspace");
      volumeMounts.add(wsVolumeMount);

      Volume wsVolume = new Volume();
      wsVolume.setName(helperKubeService.getPrefixVol() + "-ws");
      PersistentVolumeClaimVolumeSource wsPVCVolumeSource = new PersistentVolumeClaimVolumeSource();
      wsPVCVolumeSource.setClaimName(getPVCName(helperKubeService.getWorkspaceLabels(workspaceId, customLabels)));
      wsVolume.setPersistentVolumeClaim(wsPVCVolumeSource);
      volumes.add(wsVolume);
    }

    if (!getPVCName(helperKubeService.getWorkflowLabels(workflowId, workflowActivityId, customLabels)).isEmpty()) {
      VolumeMount wfVolumeMount = new VolumeMount();
      wfVolumeMount.setName(helperKubeService.getPrefixVol() + "-wf");
      wfVolumeMount.setMountPath("/workflow");
      volumeMounts.add(wfVolumeMount);
      
      Volume wfVolume = new Volume();
      wfVolume.setName(helperKubeService.getPrefixVol() + "-wf");
      PersistentVolumeClaimVolumeSource wfPVCVolumeSource = new PersistentVolumeClaimVolumeSource();
      wfPVCVolumeSource.setClaimName(getPVCName(helperKubeService.getWorkflowLabels(workflowId, workflowActivityId, customLabels)));
      wfVolume.setPersistentVolumeClaim(wfPVCVolumeSource);
      volumes.add(wfVolume);
    }
    
    /*
     * The following code is integrated to the helm chart and CICD properties It allows for
     * containers that breach the standard ephemeral-storage size by off-loading to memory See:
     * https://kubernetes.io/docs/concepts/storage/volumes/#emptydir
     */
    VolumeMount dataVolumeMount = new VolumeMount();
    dataVolumeMount.setName(helperKubeService.getPrefixVol() + "-data");
    dataVolumeMount.setMountPath("/data");
    volumeMounts.add(dataVolumeMount);
    
    Volume dataVolume = new Volume();
    dataVolume.setName(helperKubeService.getPrefixVol() + "-data");
    EmptyDirVolumeSource dataEmptyDirVolumeSource = new EmptyDirVolumeSource();
    if (kubeWorkerStorageDataMemory
        && Boolean.valueOf(taskProperties.get("worker.storage.data.memory"))) {
      LOGGER.info("Setting /data to in memory storage...");
      dataEmptyDirVolumeSource.setMedium("Memory");
    }
    dataVolume.setEmptyDir(dataEmptyDirVolumeSource);
    volumes.add(dataVolume);

    /*
     * Creation of property projected volume to mount workflow and task configmaps
     */
    VolumeMount propsVolumeMount = new VolumeMount();
    propsVolumeMount.setName(helperKubeService.getPrefixVol() + "-props");
    propsVolumeMount.setMountPath("/props");
    volumeMounts.add(propsVolumeMount);
    
    Volume propsVolume = new Volume();
    propsVolume.setName(helperKubeService.getPrefixVol() + "-props");
    ProjectedVolumeSource projectedVolPropsSource = new ProjectedVolumeSource();
    List<VolumeProjection> projectPropsVolumeList = new ArrayList<>();
    VolumeProjection wfCMVolumeProjection = new VolumeProjection();
    ConfigMapProjection projectedWFConfigMap = new ConfigMapProjection();
    projectedWFConfigMap.setName(getConfigMapName(helperKubeService.getWorkflowLabels(workflowId, workflowActivityId, customLabels)));
    wfCMVolumeProjection.setConfigMap(projectedWFConfigMap);    
    projectPropsVolumeList.add(wfCMVolumeProjection);
    VolumeProjection taskCMVolumeProjection = new VolumeProjection();
    ConfigMapProjection projectedTaskConfigMap = new ConfigMapProjection();
    projectedTaskConfigMap.setName(getConfigMapName(helperKubeService.getTaskLabels( workflowId, workflowActivityId, taskId, taskActivityId, customLabels)));
    taskCMVolumeProjection.setConfigMap(projectedTaskConfigMap);    
    projectPropsVolumeList.add(taskCMVolumeProjection);
    projectedVolPropsSource.setSources(projectPropsVolumeList);
    propsVolume.setProjected(projectedVolPropsSource);
    volumes.add(propsVolume);
    
    /*
     * Create the main task container
     */
    List<Container> containers = new ArrayList<>();
    Container taskContainer = new Container();
    taskContainer.setName("task-cntr");
    taskContainer.setImage(image);
    List<String> commands = new ArrayList<>();
    if (command != null && !command.isEmpty()) {
      commands.add(command);
    }
    commands.add("");
    taskContainer.setCommand(commands);
    taskContainer.setImagePullPolicy(kubeImagePullPolicy);
    taskContainer.setArgs(arguments);
    taskContainer.setEnv(envVars);
    taskContainer.setVolumeMounts(volumeMounts);
    taskContainer.setSecurityContext(securityContext);
    taskContainer.setResources(resources);
    containers.add(taskContainer);
    
    /*
     * The following code is for tasks with Lifecycle Watcher enabled.
     * - initContainer
     * - lifecycleContainer
     */
    List<Container> initContainers = new ArrayList<>();
    if (createLifecycleWatcher) {
      Container initContainer = new Container();
      initContainer.setName("init-cntr");
      initContainer.setImage(kubeLifecycleImage);
      initContainer.setImagePullPolicy(kubeImagePullPolicy);
      List<String> initArgs = new ArrayList<>();
      initArgs.add("lifecycle");
      initArgs.add("init");
      initContainer.setArgs(initArgs);
      List<VolumeMount> lifecycleVolumeMounts = new ArrayList<>();
      VolumeMount lifecycleVolumeMount = new VolumeMount();
      lifecycleVolumeMount.setName(helperKubeService.getPrefixVol() + "-lifecycle");
      lifecycleVolumeMount.setMountPath("/lifecycle");
      lifecycleVolumeMounts.add(lifecycleVolumeMount);
      initContainer.setVolumeMounts(lifecycleVolumeMounts);
      initContainers.add(initContainer);
      
      Container lifecycleContainer = new Container();
      lifecycleContainer.setName("lifecycle-cntr");
      lifecycleContainer.setImage(kubeLifecycleImage);
      lifecycleContainer.setImagePullPolicy(kubeImagePullPolicy);
      List<String> lifecycleArgs = new ArrayList<>();
      lifecycleArgs.add("lifecycle");
      lifecycleArgs.add("wait");
      lifecycleContainer.setArgs(lifecycleArgs);
      lifecycleVolumeMounts.add(propsVolumeMount);
      lifecycleContainer.setVolumeMounts(lifecycleVolumeMounts);
      List<EnvVar> lifecyleEnvVars = new ArrayList<>();
      lifecyleEnvVars.addAll(helperKubeService.createEnvVars(workflowId, workflowActivityId, taskName, taskId, taskActivityId));
      lifecyleEnvVars.add(helperKubeService.createEnvVar("DEBUG", taskEnableDebug.toString()));
      lifecycleContainer.setEnv(lifecyleEnvVars);
      containers.add(lifecycleContainer);

      Volume lifecycleVol = new Volume();
      lifecycleVol.setName(helperKubeService.getPrefixVol() + "-lifecycle");
      EmptyDirVolumeSource lifecycleEmptyDirVolumeSource = new EmptyDirVolumeSource();
      lifecycleVol.setEmptyDir(lifecycleEmptyDirVolumeSource);
      volumes.add(lifecycleVol);
    }
    
    /*
     * Configure Node Selector and Tolerations if defined
     */
    List<Toleration> tolerations = new ArrayList<>();
    Map<String, String> nodeSelectors = new HashMap<>();
    if (kubeJobDedicatedNodes) {
      Toleration toleration = new Toleration();
      toleration.setKey("dedicated");
      toleration.setValue("bmrg-worker");
      toleration.setEffect("NoSchedule");
      toleration.setOperator("Equal");
      tolerations.add(toleration);
      nodeSelectors.put("node-role.kubernetes.io/bmrg-worker", "true");
    }
    
    /*
     * Create Host Aliases if defined
     */
    List<HostAlias> hostAliases = new ArrayList<>();
    if (!kubeHostAliases.isEmpty()) {
      Type listHostAliasType = new TypeToken<List<HostAlias>>() {}.getType();
      List<HostAlias> hostAliasList = new Gson().fromJson(kubeHostAliases, listHostAliasType);
      LOGGER.debug("Host Alias List Size: " + hostAliasList.size());
    }
    
    /*
     * Define Image Pull Secrets
     */
    LocalObjectReference imagePullSecret = new LocalObjectReference();
    imagePullSecret.setName(kubeImagePullSecret);
    List<LocalObjectReference> imagePullSecrets = new ArrayList<>();
    imagePullSecrets.add(imagePullSecret);

    /*
     * Job Specification builder
     */
    Job job = new JobBuilder()
        .withApiVersion("batch/v1")
        .withNewMetadata()
        .withGenerateName(helperKubeService.getPrefixTask() + "-" + taskActivityId + "-")
        .withLabels(helperKubeService.getTaskLabels(workflowId, workflowActivityId, taskId, taskActivityId, customLabels))
        .withAnnotations(helperKubeService.getAnnotations("task", workflowName, workflowId,
            workflowActivityId, taskId, taskActivityId))
        .endMetadata()
        .withNewSpec()
        .withNewTemplate()
        .withNewMetadata()
        .withLabels(helperKubeService.getTaskLabels(workflowId, workflowActivityId, taskId, taskActivityId, customLabels))
        .withAnnotations(helperKubeService.getAnnotations("task", workflowName, workflowId,
            workflowActivityId, taskId, taskActivityId))
        .endMetadata()
        .withNewSpec()
        .withInitContainers(initContainers)
        .withContainers(containers)
        .withVolumes(volumes)
        .withRestartPolicy(kubeJobRestartPolicy)
        .withTolerations(tolerations)
        .withNodeSelector(nodeSelectors)
        .withAffinity(helperKubeService.getPodAffinity(helperKubeService.createAntiAffinityLabels("task")))
        .withHostAliases(hostAliases)
        .withNewServiceAccountName(kubeJobServiceAccount)
        .withImagePullSecrets(imagePullSecrets)
        .endSpec()
        .endTemplate()
        .withBackoffLimit(kubeJobBackOffLimit)
        .withTtlSecondsAfterFinished(ONE_DAY_IN_SECONDS * kubeJobTTLDays)
        .endSpec()
        .build();
    
    Job result = client.batch().jobs().create(job);
    
    client.batch().jobs().withLabels(helperKubeService.getTaskLabels(workflowId, workflowActivityId, taskId, taskActivityId, customLabels)).waitUntilReady(waitSeconds, TimeUnit.SECONDS);

    return result;
  }
  
  public void watchJob(String workflowId, String workflowActivityId, String taskId, String taskActivityId, Map<String, String> customLabels) throws InterruptedException {
    final CountDownLatch latch = new CountDownLatch(1);
    
//    Watch jobWatcher = client.pods.withLabels(helperKubeService.getTaskLabels(workflowId, workflowActivityId, taskId, taskActivityId, customLabels)).watch(new Watcher<Job>() {
//      @Override
//      public void eventReceived(Action action, Job resource) {
//        LOGGER.info("Watch event received {}: {}", action.name(), resource.getMetadata().getName());
//        switch (action.name()) {
//          case "MODIFIED":
//            LOGGER.info(resource.getStatus().toString());
//            if (resource.getStatus().getFailed() > 0) {
//              jobLatch.countDown();
//              throw new BoomerangException(BoomerangError.JOB_CREATION_ERROR, resource.getStatus().getConditions().get(0).getMessage());
//            }
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
    
    Watch podWatcher = client.pods().withLabels(helperKubeService.getTaskLabels(workflowId, workflowActivityId, taskId, taskActivityId, customLabels)).watch(new Watcher<Pod>() {
      @Override
      public void eventReceived(Action action, Pod resource) {
        LOGGER.info("Watch event received {}: {}", action.name(), resource.getMetadata().getName());
        LOGGER.info("  Pod: " + resource.getMetadata().getName() + ", started: " + resource.getStatus().getStartTime());
        LOGGER.info("  Pod Phase: " + resource.getStatus().getPhase() + "...");
        if (resource.getStatus().getConditions() != null) {
          for (PodCondition condition : resource.getStatus().getConditions()) {
            LOGGER.info("  Pod Condition: " + condition.toString());
          }
        }
        switch (action.name()) {
          case "MODIFIED":
            LOGGER.info(resource.getStatus().toString());
//            if (resource.getStatus().getFailed() > 0) {
//              jobLatch.countDown();
//              throw new BoomerangException(BoomerangError.JOB_CREATION_ERROR, resource.getStatus().getConditions().get(0).getMessage());
//            }
              break;
        }
      }
      
//    if (item.object.getStatus().getContainerStatuses() != null) {
//    for (V1ContainerStatus containerStatus : item.object.getStatus().getContainerStatuses()) {
//      LOGGER.info("Container Status: " + containerStatus.toString());
//      if ("task-cntr".equalsIgnoreCase(containerStatus.getName())
//          && containerStatus.getState().getTerminated() != null) {
//        LOGGER.info("-----------------------------------------------");
//        LOGGER.info("------- Executing Lifecycle Termination -------");
//        LOGGER.info("-----------------------------------------------");
//        try {
//          execJobLifecycle(name, "lifecycle-cntr");
//        } catch (Exception e) {
//          LOGGER.error("Lifecycle Execution Exception: ", e);
//          throw new KubeRuntimeException("Lifecycle Execution Exception", e);
//        }
//        pod = item.object;
//        break;
//      } else if ("task-cntr".equalsIgnoreCase(containerStatus.getName())
//          && containerStatus.getState().getWaiting() != null 
////          && "CreateContainerError".equalsIgnoreCase(containerStatus.getState().getWaiting().getReason())) {
//            && ArrayUtils.contains(waitingErrorReasons, containerStatus.getState().getWaiting().getReason())) {
//        throw new KubeRuntimeException("Container Waiting Error (" + containerStatus.getState().getWaiting().getReason() + ")");
//      }
//    }
//  }
      
//      private void execJobLifecycle(String podName, String containerName)  {
//        Exec exec = new Exec();
//        exec.setApiClient(Configuration.getDefaultApiClient());
//        // boolean tty = System.console() != null;
//        // String[] commands = new String[] {"node", "cli", "lifecycle", "terminate"};
//        String[] commands =
//            new String[] {"/bin/sh", "-c", "rm -f /lifecycle/lock && ls -ltr /lifecycle"};
//        LOGGER.info("Pod: " + podName + ", Container: " + containerName + ", Commands: "
//            + Arrays.toString(commands));
//        final Process proc =
//            exec.exec(kubeNamespace, podName, commands, containerName, false, false);
//
//        // Thread in =
//        // new Thread(
//        // new Runnable() {
//        // public void run() {
//        // try {
//        // ByteStreams.copy(System.in, proc.getOutputStream());
//        // } catch (IOException ex) {
//        // ex.printStackTrace();
//        // }
//        // }
//        // });
//        // in.start();
//
//        Thread out = new Thread(new Runnable() {
//          public void run() {
//            try {
//              ByteStreams.copy(proc.getInputStream(), System.out);
//            } catch (IOException ex) {
//              ex.printStackTrace();
//            }
//          }
//        });
//        out.start();
//
//        proc.waitFor();
//        // wait for any last output; no need to wait for input thread
//        out.join();
//        proc.destroy();
//      }

      @Override
      public void onClose(WatcherException e) {
        LOGGER.error("Watch error received: {}", e.getMessage(), e);
        //Cause the pod to restart
        System.exit(1);
      }

      @Override
      public void onClose() {
        LOGGER.info("Watch gracefully closed");
      }
    });
    
    latch.await(3, TimeUnit.MINUTES);
//    jobWatcher.close();
    podWatcher.close();
  }
  
  public void deleteJob(TaskDeletionEnum taskDeletion, String workflowId,
      String workflowActivityId, String taskId, String taskActivityId, Map<String, String> customLabels) {

    LOGGER.debug("Deleting Job...");
    
    client.batch().jobs().withLabels(helperKubeService.getTaskLabels(workflowId, workflowActivityId, taskId, taskActivityId, customLabels)).withPropagationPolicy(DeletionPropagation.BACKGROUND).delete();
  }
}
