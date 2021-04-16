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
import io.fabric8.knative.internal.pkg.apis.Condition;
import io.fabric8.kubernetes.api.model.ConfigMapProjection;
import io.fabric8.kubernetes.api.model.DeletionPropagation;
import io.fabric8.kubernetes.api.model.EmptyDirVolumeSource;
import io.fabric8.kubernetes.api.model.EnvVar;
import io.fabric8.kubernetes.api.model.HostAlias;
import io.fabric8.kubernetes.api.model.LocalObjectReference;
import io.fabric8.kubernetes.api.model.PersistentVolumeClaimVolumeSource;
import io.fabric8.kubernetes.api.model.ProjectedVolumeSource;
import io.fabric8.kubernetes.api.model.Toleration;
import io.fabric8.kubernetes.api.model.Volume;
import io.fabric8.kubernetes.api.model.VolumeMount;
import io.fabric8.kubernetes.api.model.VolumeProjection;
import io.fabric8.kubernetes.client.Watch;
import io.fabric8.tekton.client.DefaultTektonClient;
import io.fabric8.tekton.client.TektonClient;
import io.fabric8.tekton.pipeline.v1beta1.ArrayOrString;
import io.fabric8.tekton.pipeline.v1beta1.Param;
import io.fabric8.tekton.pipeline.v1beta1.ParamSpec;
import io.fabric8.tekton.pipeline.v1beta1.Step;
import io.fabric8.tekton.pipeline.v1beta1.TaskRun;
import io.fabric8.tekton.pipeline.v1beta1.TaskRunBuilder;
import io.fabric8.tekton.v1beta1.internal.pipeline.pkg.apis.pipeline.pod.Template;
import net.boomerangplatform.error.BoomerangError;
import net.boomerangplatform.error.BoomerangException;
import net.boomerangplatform.model.TaskConfiguration;
import net.boomerangplatform.model.TaskDeletionEnum;

@Component
public class TektonServiceImpl {

  private static final Logger LOGGER = LogManager.getLogger(TektonServiceImpl.class);

  @Autowired
  protected NewHelperKubeServiceImpl helperKubeService;
  
  @Autowired
  protected NewKubeServiceImpl kubeService;
    
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

  TektonClient client = null;

  public TektonServiceImpl() {
    this.client = new DefaultTektonClient();
  }
  
  public TaskRun createTaskRun(boolean createLifecycleWatcher, String workspaceId, String workflowName,
      String workflowId, String workflowActivityId, String taskActivityId, String taskName,
      String taskId, Map<String, String> customLabels, List<String> arguments,
      Map<String, String> taskParameters, String image, String command,
      TaskConfiguration taskConfiguration, long waitSeconds) throws InterruptedException {

    LOGGER.info("Initializing Task...");
    
    /*
     * Define environment variables made up of
     * TODO: securityContext.setProcMount("Unmasked");
     *  - Only works with Kube 1.12. ICP 3.1.1 is Kube 1.11.5
     * TODO: determine if we need to do no network as an option
     */
//    SecurityContext securityContext = new SecurityContext();
//    securityContext.setPrivileged(true);
    
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
//    ResourceRequirements resources = new ResourceRequirements();
//    Map<String, Quantity> resourceRequests = new HashMap<>();
//    resourceRequests.put("ephemeral-storage", new Quantity(kubeResourceRequestEphemeralStorage));
//    resourceRequests.put("memory", new Quantity(kubeResourceRequestMemory));
//    Map<String, Quantity> resourceLimits = new HashMap<>();
//    resourceLimits.put("ephemeral-storage", new Quantity(kubeResourceLimitEphemeralStorage));
//    String kubeResourceLimitMemoryQuantity = taskProperties.get("worker.resource.memory.size");
//    if (kubeResourceLimitMemoryQuantity != null && !(Integer.valueOf(kubeResourceLimitMemoryQuantity.replace("Gi", "")) > 32)) {
//      LOGGER.info("Setting Resource Memory Limit to " + kubeResourceLimitMemoryQuantity + "...");
//      resourceLimits.put("memory", new Quantity(kubeResourceLimitMemoryQuantity));
//    } else {
//      LOGGER
//          .info("Setting Resource Memory Limit to default of: " + kubeResourceLimitMemory + " ...");
//      resourceLimits.put("memory", new Quantity(kubeResourceLimitMemory));
//    }
//    resources.setLimits(resourceLimits);
    
    /*
     * Create volumes and Volume Mounts
     * - /workspace for cross workflow persistence such as caches (optional if mounted prior) 
     * - /workflow for workflow based sharing between tasks (optional if mounted prior) 
     * - /props for mounting config_maps 
     * - /data for task storage (optional - needed if using in memory storage)
     */
    List<VolumeMount> volumeMounts = new ArrayList<>();
    List<Volume> volumes = new ArrayList<>();
    if (workspaceId != null && !workspaceId.isEmpty() && kubeService.checkWorkspacePVCExists(workspaceId, false)) {
      VolumeMount wsVolumeMount = new VolumeMount();
      wsVolumeMount.setName(helperKubeService.getPrefixVol() + "-ws");
      wsVolumeMount.setMountPath("/workspace");
      volumeMounts.add(wsVolumeMount);

      Volume wsVolume = new Volume();
      wsVolume.setName(helperKubeService.getPrefixVol() + "-ws");
      PersistentVolumeClaimVolumeSource wsPVCVolumeSource = new PersistentVolumeClaimVolumeSource();
      wsPVCVolumeSource.setClaimName(kubeService.getPVCName(helperKubeService.getWorkspaceLabels(workspaceId, customLabels)));
      wsVolume.setPersistentVolumeClaim(wsPVCVolumeSource);
      volumes.add(wsVolume);
    }

    if (!kubeService.getPVCName(helperKubeService.getWorkflowLabels(workflowId, workflowActivityId, customLabels)).isEmpty()) {
      VolumeMount wfVolumeMount = new VolumeMount();
      wfVolumeMount.setName(helperKubeService.getPrefixVol() + "-wf");
      wfVolumeMount.setMountPath("/workflow");
      volumeMounts.add(wfVolumeMount);
      
      Volume wfVolume = new Volume();
      wfVolume.setName(helperKubeService.getPrefixVol() + "-wf");
      PersistentVolumeClaimVolumeSource wfPVCVolumeSource = new PersistentVolumeClaimVolumeSource();
      wfPVCVolumeSource.setClaimName(kubeService.getPVCName(helperKubeService.getWorkflowLabels(workflowId, workflowActivityId, customLabels)));
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
        && Boolean.valueOf(taskParameters.get("worker.storage.data.memory"))) {
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
    projectedWFConfigMap.setName(kubeService.getConfigMapName(helperKubeService.getWorkflowLabels(workflowId, workflowActivityId, customLabels)));
    wfCMVolumeProjection.setConfigMap(projectedWFConfigMap);    
    projectPropsVolumeList.add(wfCMVolumeProjection);
    VolumeProjection taskCMVolumeProjection = new VolumeProjection();
    ConfigMapProjection projectedTaskConfigMap = new ConfigMapProjection();
    projectedTaskConfigMap.setName(kubeService.getConfigMapName(helperKubeService.getTaskLabels( workflowId, workflowActivityId, taskId, taskActivityId, customLabels)));
    taskCMVolumeProjection.setConfigMap(projectedTaskConfigMap);    
    projectPropsVolumeList.add(taskCMVolumeProjection);
    projectedVolPropsSource.setSources(projectPropsVolumeList);
    propsVolume.setProjected(projectedVolPropsSource);
    volumes.add(propsVolume);
       
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
    
//    TaskSpec taskSpec = new TaskSpec();
    List<ParamSpec> taskSpecParams = new ArrayList<>();
    List<Param> taskParams = new ArrayList<>();
    taskParameters.forEach((key, value) -> {
      ParamSpec taskSpecParam = new ParamSpec();
      taskSpecParam.setName(key);
      taskSpecParam.setType("string");
      taskSpecParams.add(taskSpecParam);
      Param taskParam = new Param();
      taskParam.setName(key);
      ArrayOrString valueString = new ArrayOrString();
      valueString.setStringVal(value);
      taskParam.setValue(valueString);
      taskParams.add(taskParam);
      envVars.add(helperKubeService.createEnvVar(key, "true"));
    });
    
    /*
     * Create the main task container
     */
    List<Step> taskSteps = new ArrayList<>();
    Step taskStep = new Step();
    taskStep.setName("task");
    taskStep.setImage(image);
    List<String> commands = new ArrayList<>();
    if (command != null && !command.isEmpty()) {
      commands.add(command);
    }
    taskStep.setCommand(commands);
    taskStep.setImagePullPolicy(kubeImagePullPolicy);
    taskStep.setArgs(arguments);
    taskStep.setEnv(envVars);
    taskStep.setVolumeMounts(volumeMounts);
//    taskStep.setSecurityContext(securityContext);
//    taskContainer.setResources(resources);
    taskSteps.add(taskStep);
    
    /*
     * Create the additional PodTemplate based controlls
     * TODO: figure out if volumes go here or on the TaskRunSpec
     */
    Template taskPodTemplate = new Template();
    taskPodTemplate.setNodeSelector(nodeSelectors);
    taskPodTemplate.setTolerations(tolerations);
    taskPodTemplate.setImagePullSecrets(imagePullSecrets);
//    taskPodTemplate.setVolumes(volumes);
    
    /*
     * Build out TaskRun definition.
     * - Optionally loads Node Selector and Tolerations
     * TODO: determine how to make Task Workspace work. Currently if using the local-path (bound to a particular node)
     * the Task doesn't find a node to allow it to run. It cant handle the standard Pod method of determine if all its
     * volumes can be satisfied
     * Notes:
     * - Parameters passed into the TaskRun MUST be parameters on the task
     */
    TaskRun taskRun = new TaskRunBuilder()
      .withNewMetadata()
      .withGenerateName(helperKubeService.getPrefixTask() + "-" + taskActivityId + "-")
      .withLabels(helperKubeService.getTaskLabels(workflowId, workflowActivityId, taskId, taskActivityId, customLabels))
      .withAnnotations(helperKubeService.getAnnotations("task", workflowName, workflowId,
          workflowActivityId, taskId, taskActivityId))
      .endMetadata()
      .withNewSpec()
      .withPodTemplate(taskPodTemplate)
      .withParams(taskParams)
      .withNewTaskSpec()
      .withParams(taskSpecParams)
//      .addNewParam().withName("channel").withType("string").endParam()
//      .addNewParam().withName("title").withType("string").endParam()
//      .addNewParam().withName("message").withType("string").endParam()
//      .addNewParam().withName("with-dash").withType("string").endParam()
//      .withStepTemplate(taskContainer)
      .withVolumes(volumes)
      .withSteps(taskSteps)
//      .addNewWorkspace()
//      .withName("flow")
//      .endWorkspace()
      .endTaskSpec()
//      .addNewWorkspace()
//      .withName("flow")
//      .withNewPersistentVolumeClaim(kubeService.getPVCName(helperKubeService.getWorkspaceLabels(workspaceId, customLabels)), false)
//      .endWorkspace()
      .endSpec()
      .build();

    
    LOGGER.info(taskRun);
    
    TaskRun result = client.v1beta1().taskRuns().create(taskRun);
    
//    client.batch().jobs().withLabels(helperKubeService.getTaskLabels(workflowId, workflowActivityId, taskId, taskActivityId, customLabels)).waitUntilReady(waitSeconds, TimeUnit.SECONDS);

    return result;
  }
  
  public void watchTask(String workflowId, String workflowActivityId, String taskId,
      String taskActivityId, Map<String, String> customLabels) throws InterruptedException {
    final CountDownLatch latch = new CountDownLatch(1);
    Condition result = null;
    
    TaskWatcher taskWatcher = new TaskWatcher(latch);

    try (Watch ignore = client
        .v1beta1().taskRuns().withLabels(helperKubeService.getTaskLabels(workflowId, workflowActivityId, taskId, taskActivityId, customLabels))
        .watch(taskWatcher)) {
      
      
      boolean taskComplete = latch.await(3, TimeUnit.MINUTES);
      if (!taskComplete) {
        throw new BoomerangException(BoomerangError.TASK_EXECUTION_ERROR, "TIMED_OUT - Task timed out while waiting for completion.");
      }
      
      result = taskWatcher.getResult();
      
      if (result != null && "True".equals(result.getStatus())) {
        LOGGER.info("Task completed successfully");
      } else {
        throw new BoomerangException(BoomerangError.TASK_EXECUTION_ERROR, result.getReason() + " - " + result.getMessage());
      }
      
    } catch (Exception e) {
      LOGGER.error(e.toString());
      throw e;
    }
  }
  
  public void deleteTask(TaskDeletionEnum taskDeletion, String workflowId,
      String workflowActivityId, String taskId, String taskActivityId, Map<String, String> customLabels) {

    LOGGER.debug("Deleting Job...");
    
    client.v1beta1().taskRuns().withLabels(helperKubeService.getTaskLabels(workflowId, workflowActivityId, taskId, taskActivityId, customLabels)).withPropagationPolicy(DeletionPropagation.BACKGROUND).delete();
  }
}
