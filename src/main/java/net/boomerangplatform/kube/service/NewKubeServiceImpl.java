package net.boomerangplatform.kube.service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.ConfigMapBuilder;
import io.fabric8.kubernetes.api.model.ConfigMapList;
import io.fabric8.kubernetes.api.model.PersistentVolumeClaim;
import io.fabric8.kubernetes.api.model.PersistentVolumeClaimBuilder;
import io.fabric8.kubernetes.api.model.PersistentVolumeClaimList;
import io.fabric8.kubernetes.api.model.Quantity;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientException;

@Component
public class NewKubeServiceImpl {

  private static final Logger LOGGER = LogManager.getLogger(NewKubeServiceImpl.class);

  @Autowired
  protected NewHelperKubeServiceImpl helperKubeService;
    
  protected static final Integer ONE_DAY_IN_SECONDS = 86400; // 60*60*24

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

  @Value("${kube.worker.node.dedicated}")
  protected Boolean kubeJobDedicatedNodes;

  @Value("${kube.worker.hostaliases}")
  protected String kubeHostAliases;

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

//  public ConfigMap createWorkflowConfigMap(String workflowName, String workflowId,
//      String workflowActivityId, Map<String, String> customLabels, Map<String, String> inputProps) {
//    
//  Map<String, String> dataMap = new HashMap<>();
//  Map<String, String> sysProps = new HashMap<>();
//  sysProps.put("controller-service-url", controllerServiceURL);
//  sysProps.put("workflow-name", workflowName);
//  sysProps.put("workflow-id", workflowId);
//  sysProps.put("workflow-activity-id", workflowActivityId);
//  dataMap.put("workflow.input.properties",
//      helperKubeService.createConfigMapProp(inputProps));
//  dataMap.put("workflow.system.properties",
//      helperKubeService.createConfigMapProp(sysProps));
//  
////  Watch cmWatcher = watchWorkflowConfigMap(client, helperKubeService.getWorkflowLabels(workflowId, workflowActivityId, customLabels));
//    
////  try (Watch ignored = watchWorkflowConfigMap(client,
////      helperKubeService.getWorkflowLabels(workflowId, workflowActivityId, customLabels));) {
//    ConfigMap configMap = new ConfigMapBuilder().withNewMetadata()
//        .withGenerateName(helperKubeService.getPrefixCM() + "-")
//        .withLabels(
//            helperKubeService.getWorkflowLabels(workflowId, workflowActivityId, customLabels))
//        .withAnnotations(helperKubeService.getAnnotations("workflow", workflowName, workflowId,
//            workflowActivityId, null, null))
//        .endMetadata().addToData(dataMap).build();
//    ConfigMap result = client.configMaps().create(configMap);
////  }
//  
//    return result;
//  }
  
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
  
//  public void retrieveLifecycleParameters(Map<String, String> labels) {
//    String podName = client.pods().withLabels(labels).list().getItems().get(0).getMetadata().getName();
//    
//    try (InputStream is = client.pods().withName(podName).inContainer("step-lifecycle").file("/lifecycle/results").read())  {
//      String result = new BufferedReader(new InputStreamReader(is)).lines().collect(Collectors.joining("\n"));
//      
//      LOGGER.info(result);
//    } catch (IOException e) {
//      // TODO Auto-generated catch block
//      e.printStackTrace();
//    } catch (KubernetesClientException e) {
//      LOGGER.info("retrieveResultParameter() - Unable to retrieve Task Results as: " + e.getMessage());
//    }
//  }
//  
//  public void terminateLifecycle(Map<String, String> labels) {
//    String podName = client.pods().withLabels(labels).list().getItems().get(0).getMetadata().getName();
//    String[] command = new String[] {"/bin/sh", "-c", "rm -f /lifecycle/lock"};
//    client.pods().withName(podName).inContainer("step-lifecycle").exec(command);
//  }
}
