package net.boomerangplatform.kube.service;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import javax.servlet.http.HttpServletResponse;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import com.google.common.io.ByteStreams;

import io.kubernetes.client.ApiException;
import io.kubernetes.client.Exec;
import io.kubernetes.client.PodLogs;
import io.kubernetes.client.models.V1ConfigMap;
import io.kubernetes.client.models.V1Container;
import io.kubernetes.client.models.V1ContainerStatus;
import io.kubernetes.client.models.V1EmptyDirVolumeSource;
import io.kubernetes.client.models.V1EnvVar;
import io.kubernetes.client.models.V1Job;
import io.kubernetes.client.models.V1JobSpec;
import io.kubernetes.client.models.V1LocalObjectReference;
import io.kubernetes.client.models.V1PersistentVolumeClaimVolumeSource;
import io.kubernetes.client.models.V1Pod;
import io.kubernetes.client.models.V1PodCondition;
import io.kubernetes.client.models.V1PodSpec;
import io.kubernetes.client.models.V1PodTemplateSpec;
import io.kubernetes.client.models.V1ProjectedVolumeSource;
import io.kubernetes.client.models.V1Volume;
import io.kubernetes.client.models.V1VolumeProjection;
import io.kubernetes.client.util.Watch;
import net.boomerangplatform.kube.exception.KubeRuntimeException;

@Service
@Profile({"live", "local"})
public class FlowKubeServiceImpl extends AbstractKubeServiceImpl {

  protected static final String ORG = "bmrg";
  
  protected static final String PRODUCT = "flow";
  
  protected static final String TIER = "worker";

  protected static final String PREFIX = ORG + "-" + PRODUCT;

  protected static final String PREFIX_JOB = PREFIX + "-" + TIER;

  protected static final String PREFIX_CFGMAP = PREFIX + "-cfg";

  protected static final String PREFIX_VOL = PREFIX + "-vol";

  protected static final String PREFIX_VOL_DATA = PREFIX_VOL + "-data";

  protected static final String PREFIX_VOL_PROPS = PREFIX_VOL + "-props";

  private static final String PREFIX_PVC = PREFIX + "-pvc";

  private static final Logger LOGGER = LogManager.getLogger(FlowKubeServiceImpl.class);

  @Value("${kube.lifecycle.image}")
  private String kubeLifecycleImage;

  @Value("${kube.api.timeout}")
  private Integer kubeApiTimeOut;

  @Override
  public String getPrefixJob() {
    return PREFIX_JOB;
  }
  
  @Override
  public String getPrefixPVC() {
    return PREFIX_PVC;
  }

  @Override
  protected V1Job createJobBody(String workflowName, String workflowId, String activityId, String taskActivityId,
      String taskName, String taskId, List<String> arguments,
      Map<String, String> taskProperties, String image, String command) {

    // Initialize Job Body
    V1Job body = new V1Job();
    body.metadata(
        getMetadata(workflowName, workflowId, activityId, taskId, getPrefixJob() + "-" + taskActivityId));

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
    envVars.add(createEnvVar("DEBUG", kubeWorkerDebug.toString()));
    envVars.add(createEnvVar("BMRG_WORKFLOW_ID", workflowId));
    envVars.add(createEnvVar("BMRG_ACTIVITY_ID", activityId));
    envVars.add(createEnvVar("BMRG_TASK_ID", taskId));
    envVars.add(createEnvVar("BMRG_TASK_NAME", taskName.replace(" ", "")));
    container.env(envVars);
    container.args(arguments);
    if (!getPVCName(workflowId, activityId).isEmpty()) {
      container.addVolumeMountsItem(getVolumeMount(PREFIX_VOL_DATA, "/data"));
      V1Volume workerVolume = getVolume(PREFIX_VOL_DATA);
      V1PersistentVolumeClaimVolumeSource workerVolumePVCSource =
          new V1PersistentVolumeClaimVolumeSource();
      workerVolume.persistentVolumeClaim(
          workerVolumePVCSource.claimName(getPVCName(workflowId, activityId)));
      podSpec.addVolumesItem(workerVolume);
    }
    
    if (Optional.ofNullable(image).isPresent()) {
    	List<V1Container> initContainers = new ArrayList<>();
    	V1Container initContainer = getContainer(kubeLifecycleImage, null).name("init-cntr").addVolumeMountsItem(getVolumeMount("lifecycle", "/lifecycle"))
    			.addArgsItem("lifecycle")
    			.addArgsItem("init");
    	initContainers.add(initContainer);
    	podSpec.setInitContainers(initContainers);
    	V1Container lifecycleContainer = getContainer(kubeLifecycleImage, null).name("lifecycle-cntr").addVolumeMountsItem(getVolumeMount("lifecycle", "/lifecycle"));
    	lifecycleContainer.addArgsItem("lifecycle");
    	lifecycleContainer.addArgsItem("wait");
    	container.addVolumeMountsItem(getVolumeMount("lifecycle", "/lifecycle"));
    	containerList.add(lifecycleContainer);
//    	V1Lifecycle lifecycle = new V1Lifecycle();
//    	V1Handler postStartHandler = new V1Handler();
//    	V1ExecAction postStartExec = new V1ExecAction();
//    	postStartExec.addCommandItem("/bin/sh");
//    	postStartExec.addCommandItem("-c");
//    	postStartExec.addCommandItem("touch /lifecycle/lock");
//    	postStartHandler.setExec(postStartExec);
//        lifecycle.setPostStart(postStartHandler);
//        V1Handler preStopHandler = new V1Handler();
//        V1ExecAction preStopExec = new V1ExecAction();
//        preStopExec.addCommandItem("/bin/sh");
//        preStopExec.addCommandItem("-c");
//        preStopExec.addCommandItem("rm -f /lifecycle/lock");
//        preStopHandler.setExec(preStopExec);
//        lifecycle.setPreStop(preStopHandler);
//        container.lifecycle(lifecycle);
        V1Volume lifecycleVol = getVolume("lifecycle");
        V1EmptyDirVolumeSource emptyDir = new V1EmptyDirVolumeSource();
        lifecycleVol.emptyDir(emptyDir);
        podSpec.addVolumesItem(lifecycleVol);
        container.addArgsItem("");
    }
    
    container.addVolumeMountsItem(getVolumeMount(PREFIX_VOL_PROPS, "/props"));

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

    containerList.add(container);
    podSpec.containers(containerList);
    V1LocalObjectReference imagePullSecret = new V1LocalObjectReference();
    imagePullSecret.name(kubeImagePullSecret);
    List<V1LocalObjectReference> imagePullSecretList = new ArrayList<>();
    imagePullSecretList.add(imagePullSecret);
    podSpec.imagePullSecrets(imagePullSecretList);
    podSpec.restartPolicy(kubeWorkerJobRestartPolicy);
    templateSpec.spec(podSpec);
    templateSpec.metadata(getMetadata(workflowName, workflowId, activityId, taskId, null));

    jobSpec.backoffLimit(kubeWorkerJobBackOffLimit);
    jobSpec.template(templateSpec);
    Integer ttl = ONE_DAY_IN_SECONDS * kubeWorkerJobTTLDays;
    LOGGER.info("Setting Job TTL at " + ttl + " seconds");
    jobSpec.setTtlSecondsAfterFinished(ttl);
    body.spec(jobSpec);

    return body;
  }
  
  public V1Job watchJob(String workflowId, String workflowActivityId, String taskId) {
    String labelSelector = getLabelSelector(workflowId, workflowActivityId, taskId);
    V1Job jobResult = null;

    // Since upgrade to Java11 the watcher stops listening for events (irrespective of timeout) and
    // does not throw exception.
    // Loop will restart watcher based on our own timer
    Integer loopCount = 1;
    long endTime = System.nanoTime()
        + TimeUnit.NANOSECONDS.convert(kubeApiTimeOut.longValue(), TimeUnit.SECONDS);
    do {
      LOGGER.info("Starting Job Watcher #" + loopCount + " for Task (" + taskId + ")...");
      try {
//        watch = createJobWatch(getBatchApi(), labelSelector);
//        jobResult = getJobResult(taskId, watch);
        Watch<V1Pod> podWatch = createPodWatch(labelSelector, getCoreApi());
        V1Pod pod = getJobPod(podWatch);
        LOGGER.info("--- Pod Status --- \n" + pod.getStatus());
      } catch (ApiException | IOException e) {
        LOGGER.error("getWatch Exception: ", e);
        throw new KubeRuntimeException("Error createWatch", e);
      }
      loopCount++;
    } while (System.nanoTime() < endTime && jobResult == null);
    if (jobResult == null) {
      // Final catch for a timeout and job still not complete.
      throw new KubeRuntimeException(
          "Task (" + taskId + ") has exceeded the maximum duration triggering failure.");
    } 
    return jobResult;
  }

  private V1Pod getJobPod(Watch<V1Pod> watch) throws IOException {
    V1Pod pod = null;
    try {
      for (Watch.Response<V1Pod> item : watch) {

        String name = item.object.getMetadata().getName();
        LOGGER.info("Pod: " + name + "...");
        LOGGER.info("Pod Start Time: " + item.object.getStatus().getStartTime() + "...");
        String phase = item.object.getStatus().getPhase();
        LOGGER.info("Pod Phase: " + phase + "...");
        if (item.object.getStatus().getConditions() != null) {
	        for (V1PodCondition condition : item.object.getStatus().getConditions()) {
	          LOGGER.info("Pod Condition: " + condition.toString() + "...");
	        }
        }
        if (item.object.getStatus().getContainerStatuses() != null) {
	        for (V1ContainerStatus containerStatus : item.object.getStatus().getContainerStatuses()) {
	          LOGGER.info("Container Status: " + containerStatus.toString() + "...");
	          if ("worker-cntr".equalsIgnoreCase(containerStatus.getName()) && containerStatus.getState().getTerminated() != null) {
	        	  try {
        			  execJobLifecycle(name, "lifecycle-cntr");
	        	  } catch (Exception e) {
	        		  LOGGER.error("Lifecycle Execution Exception: ", e);
	        	        throw new KubeRuntimeException("Lifecycle Execution Exception", e);
	        	  }
	        	  
	          }
	        }
        }
//        if (!("pending".equalsIgnoreCase(phase) || "unknown".equalsIgnoreCase(phase))) {
//          LOGGER.info("Pod " + name + " ready to stream logs...");
//          pod = item.object;
//          break;
//        }
      }
    } finally {
      watch.close();
    }
    return pod;
  }
  
  private void execJobLifecycle(String podName, String containerName) throws ApiException, IOException, InterruptedException {
	    Exec exec = new Exec();
//	    exec.setApiClient(getApiClient());
//	    boolean tty = System.console() != null;
	    String[] commands = new String[] {"sh", "-c", "less /lifecycle/env"};
	    // final Process proc = exec.exec("default", "nginx-4217019353-k5sn9", new String[]
	    //   {"sh", "-c", "echo foo"}, true, tty);
	    LOGGER.info("Pod: " + podName + ", Container: " + containerName + ", Commands: " + commands);
	    final Process proc =
	        exec.exec(
	        	kubeNamespace,
	            podName,
	            commands,
	            containerName,
	            false,
	            false);

//	    Thread in =
//	        new Thread(
//	            new Runnable() {
//	              public void run() {
//	                try {
//	                  ByteStreams.copy(System.in, proc.getOutputStream());
//	                } catch (IOException ex) {
//	                  ex.printStackTrace();
//	                }
//	              }
//	            });
//	    in.start();

	    Thread out =
	        new Thread(
	            new Runnable() {
	              public void run() {
	                try {
	                  ByteStreams.copy(proc.getInputStream(), System.out);
	                } catch (IOException ex) {
	                  ex.printStackTrace();
	                }
	              }
	            });
	    out.start();

	    proc.waitFor();

	    // wait for any last output; no need to wait for input thread
	    out.join();

	    proc.destroy();
  }

  protected V1ConfigMap createTaskConfigMapBody(String workflowName, String workflowId,
      String workflowActivityId, String taskName, String taskId, Map<String, String> inputProps) {
    V1ConfigMap body = new V1ConfigMap();

    body.metadata(
        getMetadata(workflowName, workflowId, workflowActivityId, taskId, PREFIX_CFGMAP));

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
        getMetadata(workflowName, workflowId, workflowActivityId, null, PREFIX_CFGMAP));

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
    StringBuilder labelSelector = new StringBuilder("platform=" + ORG + ",product=" + PRODUCT + ",tier=" + TIER);
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
    annotations.put("boomerangplatform.net/product", PRODUCT);
    annotations.put("boomerangplatform.net/tier", TIER);
    annotations.put("boomerangplatform.net/workflow-name", workflowName);
    Optional.ofNullable(workflowId)
    .ifPresent(str -> annotations.put("boomerangplatform.net/workflow-id", str));
    Optional.ofNullable(activityId)
    .ifPresent(str -> annotations.put("boomerangplatform.net/activity-id", str));
    Optional.ofNullable(taskId)
        .ifPresent(str -> annotations.put("boomerangplatform.net/task-id", str));

    return annotations;
  }

  protected Map<String, String> createLabels(String workflowId, String activityId, String taskId) {
    Map<String, String> labels = new HashMap<>();
    labels.put("platform", ORG);
    labels.put("product", PRODUCT);
    labels.put("tier", TIER);
    Optional.ofNullable(workflowId).ifPresent(str -> labels.put("workflow-id", str));
    Optional.ofNullable(activityId).ifPresent(str -> labels.put("activity-id", str));
    Optional.ofNullable(taskId).ifPresent(str -> labels.put("task-id", str));
    return labels;
  }

  @Override
  public StreamingResponseBody streamPodLog(HttpServletResponse response, String workflowId,
      String workflowActivityId, String taskId, String taskActivityId) {
	  
	LOGGER.info("Stream logging type is: " + loggingType);

    String labelSelector = getLabelSelector(workflowId, workflowActivityId, taskId);
    StreamingResponseBody responseBody = null;
    try {
      List<V1Pod> allPods =
          getCoreApi().listNamespacedPod(kubeNamespace, kubeApiIncludeuninitialized, kubeApiPretty,
              null, null, labelSelector, null, null, TIMEOUT_ONE_MINUTE, false).getItems();

      if (allPods.isEmpty() && streamLogsFromElastic()) {
    	LOGGER.error("All Pods is empty.");
        return getExternalLogs(taskActivityId);
      }

      Watch<V1Pod> watch = createPodWatch(labelSelector, getCoreApi());
      V1Pod pod = getPod(watch);
      
      if (pod == null) {
    	  LOGGER.error("V1Pod is empty...");
      }
      else {
    	  if (pod.getStatus() == null) {
    		  LOGGER.error("Pod Status is empty");
    	  }
    	  else {
    		  LOGGER.info("Phase: " + pod.getStatus().getPhase());
    	  }
      }

      if (pod == null || "succeeded".equalsIgnoreCase(pod.getStatus().getPhase())
          || "failed".equalsIgnoreCase(pod.getStatus().getPhase())) {
    	  if (streamLogsFromElastic()) {
    		  return getExternalLogs(taskActivityId);
    	  }
      }
  
      PodLogs logs = new PodLogs();
      InputStream inputStream = logs.streamNamespacedPodLog(pod);

      responseBody = getPodLog(inputStream, pod.getMetadata().getName());
    } catch (ApiException | IOException e) {
      LOGGER.error("streamPodLog Exception: ", e);
      throw new KubeRuntimeException("Error streamPodLog", e);
    }

    return responseBody;
  }
  
}
