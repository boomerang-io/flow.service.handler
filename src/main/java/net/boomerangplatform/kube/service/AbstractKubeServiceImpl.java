package net.boomerangplatform.kube.service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.elasticsearch.action.search.ClearScrollRequest;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchScrollRequest;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.Scroll;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.support.MessageSourceAccessor;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import com.google.common.io.ByteStreams;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;

import io.kubernetes.client.ApiClient;
import io.kubernetes.client.ApiException;
import io.kubernetes.client.PodLogs;
import io.kubernetes.client.apis.BatchV1Api;
import io.kubernetes.client.apis.CoreV1Api;
import io.kubernetes.client.custom.Quantity;
import io.kubernetes.client.models.V1ConfigMap;
import io.kubernetes.client.models.V1ConfigMapList;
import io.kubernetes.client.models.V1ConfigMapProjection;
import io.kubernetes.client.models.V1Container;
import io.kubernetes.client.models.V1ContainerStatus;
import io.kubernetes.client.models.V1DeleteOptions;
import io.kubernetes.client.models.V1EnvVar;
import io.kubernetes.client.models.V1Job;
import io.kubernetes.client.models.V1JobStatus;
import io.kubernetes.client.models.V1ObjectMeta;
import io.kubernetes.client.models.V1PersistentVolumeClaim;
import io.kubernetes.client.models.V1PersistentVolumeClaimList;
import io.kubernetes.client.models.V1PersistentVolumeClaimSpec;
import io.kubernetes.client.models.V1PersistentVolumeClaimStatus;
import io.kubernetes.client.models.V1Pod;
import io.kubernetes.client.models.V1PodCondition;
import io.kubernetes.client.models.V1ResourceRequirements;
import io.kubernetes.client.models.V1SecurityContext;
import io.kubernetes.client.models.V1Status;
import io.kubernetes.client.models.V1Volume;
import io.kubernetes.client.models.V1VolumeMount;
import io.kubernetes.client.models.V1VolumeProjection;
import io.kubernetes.client.util.Watch;
import net.boomerangplatform.kube.exception.KubeRuntimeException;

public abstract class AbstractKubeServiceImpl implements AbstractKubeService { // NOSONAR

  private static final Logger LOGGER = LogManager.getLogger(AbstractKubeService.class);

  private static final int PROPERTY_SIZE = 2;

  private static final Pattern PATTERN_PROPERTIES = Pattern.compile("\\s*\\n\\s*");

  private static final int TIMEOUT = 600;

  private static final int BYTE_SIZE = 1024;

  private static final int TIMEOUT_ONE_MINUTE = 60;

  private static final String EXCEPTION = "Exception: ";

  protected static final Integer ONE_DAY_IN_SECONDS = 86400; // 60*60*24

  @Value("${kube.api.base.path}")
  protected String kubeApiBasePath;

  @Value("${kube.api.token}")
  protected String kubeApiToken;

  @Value("${kube.api.debug}")
  protected Boolean kubeApiDebug;

  @Value("${kube.api.type}")
  protected String kubeApiType;

  @Value("${kube.api.pretty}")
  private String kubeApiPretty;

  @Value("${kube.api.includeunitialized}")
  protected Boolean kubeApiIncludeuninitialized;

  @Value("${kube.api.timeout}")
  private Integer kubeApiTimeOut;

  @Value("${kube.namespace}")
  protected String kubeNamespace;

  @Value("${kube.image.pullPolicy}")
  protected String kubeImagePullPolicy;

  @Value("${kube.image.pullSecret}")
  protected String kubeImagePullSecret;

  @Value("${kube.worker.pvc.initialSize}")
  protected String kubeWorkerPVCInitialSize;

  @Value("${kube.worker.job.backOffLimit}")
  protected Integer kubeWorkerJobBackOffLimit;

  @Value("${kube.worker.job.restartPolicy}")
  protected String kubeWorkerJobRestartPolicy;

  @Value("${kube.worker.job.ttlDays}")
  protected Integer kubeWorkerJobTTLDays;

  @Value("${kube.worker.debug}")
  protected Boolean kubeWorkerDebug;

  @Value("${kube.worker.hostaliases}")
  protected String kubeWorkerHostAliases;

  @Value("${kube.worker.serviceaccount}")
  protected String kubeWorkerServiceAccount;

  @Value("${proxy.enable}")
  protected Boolean proxyEnabled;

  @Value("${proxy.host}")
  protected String proxyHost;

  @Value("${proxy.port}")
  protected String proxyPort;

  @Value("${proxy.ignore}")
  protected String proxyIgnore;

  @Value("${controller.service.host}")
  protected String bmrgControllerServiceURL;

  @Value("${kube.image}")
  private String kubeImage;

  @Value("${kube.worker.logging.type}")
  protected String loggingType;

  @Autowired
  private MessageSource messageSource;

  @Autowired
  @Lazy
  private RestHighLevelClient elasticRestClient;


  private ApiClient apiClient; // NOSONAR

  protected abstract String getLabelSelector(String workflowId, String workflowActivityId,
      String taskId);

  protected abstract V1Job createJobBody(String workflowName, String workflowId,
      String workflowActivityId, String taskName, String taskId, List<String> arguments,
      Map<String, String> taskInputProperties, String image, String command);

  protected abstract V1ConfigMap createTaskConfigMapBody(String workflowName, String workflowId,
      String workflowActivityId, String taskName, String taskId, Map<String, String> inputProps);

  protected abstract Map<String, String> createAnnotations(String workflowName, String workflowId,
      String workflowActivityId, String taskId);

  protected abstract Map<String, String> createLabels(String workflowId, String workflowActivityId,
      String taskId);

  protected abstract V1ConfigMap createWorkflowConfigMapBody(String workflowName, String workflowId,
      String workflowActivityId, Map<String, String> inputProps);

  public abstract String getPrefixJob();

  public abstract String getPrefixPVC();

  @Override
  public V1Job createJob(String workflowName, String workflowId, String workflowActivityId,
      String taskName, String taskId, List<String> arguments,
      Map<String, String> taskProperties) {
    V1Job body = createJobBody(workflowName, workflowId, workflowActivityId, taskName, taskId,
        arguments, taskProperties, null, null);

    V1Job jobResult = new V1Job();
    try {
      jobResult = getBatchApi().createNamespacedJob(kubeNamespace, body,
          kubeApiIncludeuninitialized, kubeApiPretty, null);
      LOGGER.info(jobResult);
    } catch (ApiException e) {
      if (e.getCause() instanceof SocketTimeoutException) {
        SocketTimeoutException ste = (SocketTimeoutException) e.getCause();
        if (ste.getMessage() != null && ste.getMessage().contains("timeout")) {
          LOGGER.warn("Catching timeout and return as task error");
          V1JobStatus badStatus = new V1JobStatus();
          return body.status(badStatus.failed(1));
        }
      }
      LOGGER.error("Error: ", e);
    }

    return jobResult;
  }
  
  @Override
  public V1Job createJob(String workflowName, String workflowId, String workflowActivityId,
      String taskName, String taskId, List<String> arguments,
      Map<String, String> taskProperties, String image, String command) {
    V1Job body = createJobBody(workflowName, workflowId, workflowActivityId, taskName, taskId,
        arguments, taskProperties, image, command);

    V1Job jobResult = new V1Job();
    try {
      jobResult = getBatchApi().createNamespacedJob(kubeNamespace, body,
          kubeApiIncludeuninitialized, kubeApiPretty, null);
      LOGGER.info(jobResult);
    } catch (ApiException e) {
      if (e.getCause() instanceof SocketTimeoutException) {
        SocketTimeoutException ste = (SocketTimeoutException) e.getCause();
        if (ste.getMessage() != null && ste.getMessage().contains("timeout")) {
          LOGGER.warn("Catching timeout and return as task error");
          V1JobStatus badStatus = new V1JobStatus();
          return body.status(badStatus.failed(1));
        }
      }
      LOGGER.error("Error: ", e);
    }

    return jobResult;
  }

  @Override
  public V1Job watchJob(String workflowId, String workflowActivityId, String taskId) {
    String labelSelector = getLabelSelector(workflowId, workflowActivityId, taskId);
    Watch<V1Job> watch;
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
        watch = createJobWatch(getBatchApi(), labelSelector);
        jobResult = getJobResult(taskId, watch);
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

  @Override
  public String getPodLog(String workflowId, String workflowActivityId, String taskId) {
    String labelSelector = getLabelSelector(workflowId, workflowActivityId, taskId);

    PodLogs logs = new PodLogs();
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    try {
      List<V1Pod> listOfPods =
          getCoreApi().listNamespacedPod(kubeNamespace, kubeApiIncludeuninitialized, kubeApiPretty,
              null, null, labelSelector, null, null, TIMEOUT_ONE_MINUTE, false).getItems();

      V1Pod pod;

      if (!listOfPods.isEmpty()) {
        pod = listOfPods.get(0);
        InputStream is = logs.streamNamespacedPodLog(pod);
        ByteStreams.copy(is, baos);
      }
    } catch (ApiException | IOException e) {
      LOGGER.error("getPodLog Exception: ", e);
      throw new KubeRuntimeException("Error getPodLog", e);
    }

    return baos.toString(StandardCharsets.UTF_8);
  }

  private boolean streamLogsFromElastic() {
	  return "elastic".equals(loggingType);
  }
  @Override
  public StreamingResponseBody streamPodLog(HttpServletResponse response, String workflowId,
      String workflowActivityId, String taskId) {

    String labelSelector = getLabelSelector(workflowId, workflowActivityId, taskId);
    StreamingResponseBody responseBody = null;
    try {
      List<V1Pod> allPods =
          getCoreApi().listNamespacedPod(kubeNamespace, kubeApiIncludeuninitialized, kubeApiPretty,
              null, null, labelSelector, null, null, TIMEOUT_ONE_MINUTE, false).getItems();

      if (allPods.isEmpty() && streamLogsFromElastic()) {
        return getExternalLogs(workflowActivityId);
      }

      Watch<V1Pod> watch = createPodWatch(labelSelector, getCoreApi());
      V1Pod pod = getPod(watch);

      if (pod == null || "succeeded".equalsIgnoreCase(pod.getStatus().getPhase())
          || "failed".equalsIgnoreCase(pod.getStatus().getPhase())) {
    	  if (streamLogsFromElastic()) {
    		  return getExternalLogs(workflowActivityId);
    	  }
    	  else {
    		  return getDefaultErrorMessage();
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

  public V1PersistentVolumeClaim createPVC(String workflowName, String workflowId,
      String workflowActivityId, String pvcSize) throws ApiException {
    // Setup
    V1PersistentVolumeClaim body = new V1PersistentVolumeClaim();

    // Create Metadata
    V1ObjectMeta metadata = new V1ObjectMeta();
    metadata.annotations(createAnnotations(workflowName, workflowId, workflowActivityId, null));
    metadata.labels(createLabels(workflowId, workflowActivityId, null));
    metadata.generateName(getPrefixPVC() + "-");
    body.metadata(metadata);

    // Create PVC Spec
    V1PersistentVolumeClaimSpec pvcSpec = new V1PersistentVolumeClaimSpec();
    List<String> pvcAccessModes = new ArrayList<>();
    pvcAccessModes.add("ReadWriteMany");
    pvcSpec.accessModes(pvcAccessModes);
    V1ResourceRequirements pvcResourceReq = new V1ResourceRequirements();
    Map<String, Quantity> pvcRequests = new HashMap<>();
    if (pvcSize == null || pvcSize.isEmpty()) {
      pvcSize = kubeWorkerPVCInitialSize;
    }
    pvcRequests.put("storage", Quantity.fromString(pvcSize));
    pvcResourceReq.requests(pvcRequests);
    pvcSpec.resources(pvcResourceReq);
    body.spec(pvcSpec);

    V1PersistentVolumeClaim result = getCoreApi().createNamespacedPersistentVolumeClaim(
        kubeNamespace, body, kubeApiIncludeuninitialized, kubeApiPretty, null);
    LOGGER.info(result);
    return result;
  }

  @Override
  public V1PersistentVolumeClaimStatus watchPVC(String workflowId, String workflowActivityId) {
    String labelSelector = getLabelSelector(workflowId, workflowActivityId, null);
    V1PersistentVolumeClaimStatus result = null;
    try {
      result =
          getPersistentVolumeClaimStatus(getPersistentVolumeWatch(getCoreApi(), labelSelector));
    } catch (ApiException | IOException e) {
      LOGGER.error("watchPVC Exception: ", e);
      throw new KubeRuntimeException("Error watchPVC", e);
    }
    return result;
  }

  private V1PersistentVolumeClaimStatus getPersistentVolumeClaimStatus(
      Watch<V1PersistentVolumeClaim> watch) throws IOException {
    V1PersistentVolumeClaimStatus result = null;
    try {
      for (Watch.Response<V1PersistentVolumeClaim> item : watch) {
        LOGGER.info(String.format("%s : %s%n", item.type, item.object.getMetadata().getName()));
        LOGGER.info(item.object.getStatus());
        result = item.object.getStatus();
        if (result != null && result.getPhase() != null && "Bound".equals(result.getPhase())) {
          break;
        }
      }

    } finally {
      watch.close();
    }
    return result;
  }

  private Watch<V1PersistentVolumeClaim> getPersistentVolumeWatch(CoreV1Api api,
      String labelSelector) throws ApiException {
    return Watch.createWatch(createWatcherApiClient(),
        api.listNamespacedPersistentVolumeClaimCall(kubeNamespace, kubeApiIncludeuninitialized,
            kubeApiPretty, null, null, labelSelector, null, null, TIMEOUT, true, null, null),
        new TypeToken<Watch.Response<V1PersistentVolumeClaim>>() {}.getType());
  }

  protected String getPVCName(String workflowId, String workflowActivityId) {
    String labelSelector = getLabelSelector(workflowId, workflowActivityId, null);

    try {
      V1PersistentVolumeClaimList persistentVolumeClaimList = getCoreApi()
          .listNamespacedPersistentVolumeClaim(kubeNamespace, kubeApiIncludeuninitialized,
              kubeApiPretty, null, null, labelSelector, null, null, TIMEOUT_ONE_MINUTE, false);
      persistentVolumeClaimList.getItems().forEach(pvc -> {
        LOGGER.info(pvc.toString());
        LOGGER.info(" PVC Name: " + pvc.getMetadata().getName());
      });
      if (!persistentVolumeClaimList.getItems().isEmpty()
          && persistentVolumeClaimList.getItems().get(0).getMetadata().getName() != null) {
        LOGGER.info("----- End getPVCName() -----");
        return persistentVolumeClaimList.getItems().get(0).getMetadata().getName();
      }
    } catch (ApiException e) {
      LOGGER.error(EXCEPTION, e);
    }
    return "";
  }

  @Override
  public V1Status deletePVC(String workflowId, String workflowActivityId) {
    V1DeleteOptions deleteOptions = new V1DeleteOptions();
    V1Status result = new V1Status();
    try {
      result = getCoreApi().deleteNamespacedPersistentVolumeClaim(
          getPVCName(workflowId, workflowActivityId), kubeNamespace, kubeApiPretty, deleteOptions,
          null, null, null, null);
    } catch (JsonSyntaxException e) {
      if (e.getCause() instanceof IllegalStateException) {
        IllegalStateException ise = (IllegalStateException) e.getCause();
        if (ise.getMessage() != null
            && ise.getMessage().contains("Expected a string but was BEGIN_OBJECT")) {
          LOGGER.error(
              "Catching exception because of issue https://github.com/kubernetes-client/java/issues/86");
        }
        LOGGER.error("Exception when running deletePVC()", e);
      }
    } catch (ApiException e) {
      LOGGER.error("Exception when running deletePVC()", e);
    }
    return result;
  }

  @Override
  public V1ConfigMap createWorkflowConfigMap(String workflowName, String workflowId,
      String workflowActivityId, Map<String, String> inputProps) {
    return createConfigMap(
        createWorkflowConfigMapBody(workflowName, workflowId, workflowActivityId, inputProps));
  }

  @Override
  public V1ConfigMap createTaskConfigMap(String workflowName, String workflowId,
      String workflowActivityId, String taskName, String taskId, Map<String, String> inputProps) {
    LOGGER.info("ConfigMapBody: " + inputProps);
    return createConfigMap(createTaskConfigMapBody(workflowName, workflowId, workflowActivityId,
        taskName, taskId, inputProps));
  }

  private V1ConfigMap createConfigMap(V1ConfigMap body) {
    V1ConfigMap result = new V1ConfigMap();
    try {
      result = getCoreApi().createNamespacedConfigMap(kubeNamespace, body,
          kubeApiIncludeuninitialized, kubeApiPretty, null);
      LOGGER.info(result);
    } catch (ApiException e) {
      LOGGER.error("Exception when calling CoreV1Api#createNamespacedConfigMap", e);
      throw new KubeRuntimeException("Error create configMap", e);
    }
    return result;
  }

  @Override
  public V1ConfigMap watchConfigMap(String workflowId, String workflowActivityId, String taskId) {
    String labelSelector = getLabelSelector(workflowId, workflowActivityId, taskId);

    try {
      Watch<V1ConfigMap> watch = Watch.createWatch(createWatcherApiClient(),
          getCoreApi().listNamespacedConfigMapCall(kubeNamespace, kubeApiIncludeuninitialized,
              kubeApiPretty, null, null, labelSelector, null, null, null, true, null, null),
          new TypeToken<Watch.Response<V1ConfigMap>>() {}.getType());

      return getConfigMapResult(watch);
    } catch (IOException | ApiException e) {
      throw new KubeRuntimeException("Error create configMap watch", e);
    }
  }

  @Override
  public void patchTaskConfigMap(String workflowId, String workflowActivityId, String taskId,
      String taskName, Map<String, String> properties) {
    V1ConfigMap wfConfigMap = getConfigMap(workflowId, workflowActivityId, null);
    String fileName = taskName + ".output.properties";
    patchConfigMap(getConfigMapName(wfConfigMap), fileName,
        getConfigMapDataProp(wfConfigMap, fileName), createConfigMapProp(properties));
  }

  @Override
  public Map<String, String> getTaskOutPutConfigMapData(String workflowId,
      String workflowActivityId, String taskId, String taskName) {
    LOGGER.info("  taskName: " + taskName);
    V1ConfigMap wfConfigMap = getConfigMap(workflowId, workflowActivityId, null);
    String fileName = taskName.replace(" ", "") + ".output.properties";
    String dataString = getConfigMapDataProp(wfConfigMap, fileName);

    Map<String, String> properties =
        PATTERN_PROPERTIES.splitAsStream(dataString.trim()).map(s -> s.split("=", PROPERTY_SIZE))
            .collect(Collectors.toMap(a -> a[0], a -> a.length > 1 ? a[1] : ""));

    LOGGER.info("  properties: " + properties.toString());
    return properties;
  }

  @Override
  public V1Status deleteConfigMap(String workflowId, String workflowActivityId, String taskId) {
    V1DeleteOptions deleteOptions = new V1DeleteOptions();
    V1Status result = new V1Status();

    try {
      result = getCoreApi().deleteNamespacedConfigMap(
          getConfigMapName(getConfigMap(workflowId, workflowActivityId, taskId)), kubeNamespace,
          kubeApiPretty, deleteOptions, null, null, null, null);
    } catch (JsonSyntaxException e) {
      if (e.getCause() instanceof IllegalStateException) {
        IllegalStateException ise = (IllegalStateException) e.getCause();
        if (ise.getMessage() != null
            && ise.getMessage().contains("Expected a string but was BEGIN_OBJECT")) {
          LOGGER.error(
              "Catching exception because of issue https://github.com/kubernetes-client/java/issues/86");
        }

        LOGGER.error("Exception when running deleteConfigMap()", e);
      }
    } catch (ApiException e) {
      LOGGER.error("Exception when running deleteConfigMap()", e);
    }
    return result;
  }

  public boolean checkPVCExists(String workflowId, String workflowActivityId, String taskId,
      boolean failIfNotBound) {
    String labelSelector = getLabelSelector(workflowId, workflowActivityId, taskId);

    boolean isPVCExists = false;
    try {
      V1PersistentVolumeClaimList persistentVolumeClaimList = getCoreApi()
          .listNamespacedPersistentVolumeClaim(kubeNamespace, kubeApiIncludeuninitialized,
              kubeApiPretty, null, null, labelSelector, null, null, TIMEOUT_ONE_MINUTE, false);
      isPVCExists = isPVCAvailable(failIfNotBound, persistentVolumeClaimList);
    } catch (ApiException e) {
      LOGGER.error(
          "No PVC found matching Id: " + workflowId + " and ActivityId: " + workflowActivityId, e);
    }
    return isPVCExists;
  }

  protected List<V1EnvVar> createProxyEnvVars() {
    List<V1EnvVar> proxyEnvVars = new ArrayList<>();

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

    return proxyEnvVars;
  }

  protected V1EnvVar createEnvVar(String key, String value) {
    V1EnvVar envVar = new V1EnvVar();
    envVar.setName(key);
    envVar.setValue(value);
    return envVar;
  }

  protected String createConfigMapProp(Map<String, String> properties) {
    LOGGER.info("Building ConfigMap Body");
    Properties props = new Properties();
    StringWriter propsSW = new StringWriter();
    if (properties != null && !properties.isEmpty()) {
      properties.forEach((key, value) -> {
        String valueStr = value != null ? value : "";
        LOGGER.info("  " + key + "=" + valueStr);
        props.setProperty(key, valueStr);
      });
    }

    try {
      props.store(propsSW, null);
      LOGGER.info("" + propsSW.toString());
    } catch (IOException ex) {
      LOGGER.error(EXCEPTION, ex);
    }

    return propsSW.toString();
  }

  protected V1ConfigMap getConfigMap(String workflowId, String workflowActivityId, String taskId) {
    V1ConfigMap configMap = null;

    String labelSelector = getLabelSelector(workflowId, workflowActivityId, taskId);
    try {
      V1ConfigMapList configMapList =
          getCoreApi().listNamespacedConfigMap(kubeNamespace, kubeApiIncludeuninitialized,
              kubeApiPretty, null, null, labelSelector, null, null, TIMEOUT_ONE_MINUTE, false);
      if (!configMapList.getItems().isEmpty()) {
        configMap = configMapList.getItems().get(0);
      }
    } catch (ApiException e) {
      LOGGER.error("Error: ", e);
    }
    return configMap;
  }

  protected String getConfigMapName(V1ConfigMap configMap) {
    String configMapName = "";

    if (configMap != null && !configMap.getMetadata().getName().isEmpty()) {
      configMapName = configMap.getMetadata().getName();
      LOGGER.info(" ConfigMap Name: " + configMapName);
    }
    return configMapName;
  }

  private V1ConfigMap getConfigMapResult(Watch<V1ConfigMap> watch) throws IOException {
    V1ConfigMap result = null;
    try {
      if (watch.hasNext()) {
        Watch.Response<V1ConfigMap> item = watch.next();
        LOGGER.info(String.format("%s : %s%n", item.type, item.object.getMetadata().getName()));
        result = item.object;
      }
    } finally {
      watch.close();
    }
    return result;
  }

  protected V1VolumeProjection getVolumeProjection(V1ConfigMap wfConfigMap) {
    V1ConfigMapProjection projectedConfigMapTask = new V1ConfigMapProjection();
    projectedConfigMapTask.name(getConfigMapName(wfConfigMap));
    V1VolumeProjection configMapVolSourceTask = new V1VolumeProjection();
    configMapVolSourceTask.configMap(projectedConfigMapTask);
    return configMapVolSourceTask;
  }

  protected V1Volume getVolume(String name) {
    V1Volume workerVolume = new V1Volume();
    workerVolume.name(name);
    return workerVolume;
  }

  protected V1VolumeMount getVolumeMount(String name, String path) {
    V1VolumeMount volMount = new V1VolumeMount();
    volMount.name(name);
    volMount.mountPath(path);
    return volMount;
  }

  protected V1Container getContainer(String image, String command) {
    V1Container container = new V1Container();
    LOGGER.info("Container Image: " + Optional.ofNullable(image).orElse(kubeImage));
    container.image(Optional.ofNullable(image).orElse(kubeImage));
    Optional.ofNullable(command).ifPresent(str -> container.addCommandItem(str));
    container.name("worker-cntr");
    container.imagePullPolicy(kubeImagePullPolicy);
    V1SecurityContext securityContext = new V1SecurityContext();
    securityContext.setPrivileged(true);
//	Only works with Kube 1.12. ICP 3.1.1 is Kube 1.11.5
//	TODO: securityContext.setProcMount("Unmasked");
    container.setSecurityContext(securityContext);
    return container;
  }

/*
 * Passes through optional method inputs to the sub methods which need to handle this.
 */
  protected V1ObjectMeta getMetadata(String workflowName, String workflowId,
      String workflowActivityId, String taskId, String generateName) {
    V1ObjectMeta metadata = new V1ObjectMeta();
    metadata.annotations(createAnnotations(workflowName, workflowId, workflowActivityId, taskId));
    metadata.labels(createLabels(workflowId, workflowActivityId, taskId));
    if (StringUtils.isNotBlank(generateName)) {
      metadata.generateName(generateName + "-");
    }
    return metadata;
  }

  private boolean isPVCAvailable(boolean failIfNotBound,
      V1PersistentVolumeClaimList persistentVolumeClaimList) {
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

  private ApiClient createWatcherApiClient() {
    // This leverages the default ApiClient in the KubeConfiguration.java Class and overrides the
    // debugging to false as Watch is (for now) incompatible with debugging mode active.
    // Watches will not return data until the watch connection terminates
    // io.kubernetes.client.ApiException: Watch is incompatible with debugging mode active.
    ApiClient watcherClient =
        io.kubernetes.client.Configuration.getDefaultApiClient().setDebugging(false);
    watcherClient.getHttpClient().setReadTimeout(kubeApiTimeOut.longValue(), TimeUnit.SECONDS);
    return watcherClient;

  }

  private Watch<V1Job> createJobWatch(BatchV1Api api, String labelSelector) throws ApiException {
    return Watch.createWatch(createWatcherApiClient(),
        api.listNamespacedJobCall(kubeNamespace, kubeApiIncludeuninitialized, kubeApiPretty, null,
            null, labelSelector, null, null, null, true, null, null),
        new TypeToken<Watch.Response<V1Job>>() {}.getType());
  }

  private Watch<V1Pod> createPodWatch(String labelSelector, CoreV1Api api) throws ApiException {
    return Watch.createWatch(createWatcherApiClient(),
        api.listNamespacedPodCall(kubeNamespace, kubeApiIncludeuninitialized, kubeApiPretty, null,
            null, labelSelector, null, null, null, true, null, null),
        new TypeToken<Watch.Response<V1Pod>>() {}.getType());
  }


  private V1Job getJobResult(String taskId, Watch<V1Job> watch) throws IOException {
    V1Job jobResult = null;
    try {
      jobResult = getJob(taskId, watch);
    } finally {
      if (watch != null) {
        watch.close();
      }
    }
    return jobResult;
  }

  private V1Job getJob(String taskId, Watch<V1Job> watch) {
    V1Job jobResult = null;
    for (Watch.Response<V1Job> item : watch) {

      LOGGER.info(item.type + " : " + item.object.getMetadata().getName());
      LOGGER.info(item.object.getStatus());
      if (item.object.getStatus().getSucceeded() != null
          && item.object.getStatus().getSucceeded() >= 1) {
        LOGGER.info(String.format("Task (%s) has succeeded.", taskId));
        jobResult = item.object;
        break;
      } else if (item.object.getStatus().getFailed() != null
          && item.object.getStatus().getFailed() >= kubeWorkerJobBackOffLimit) {
        throw new KubeRuntimeException("Task (" + taskId + ") has failed to execute "
            + kubeWorkerJobBackOffLimit + " times triggering failure.");
      }
    }

    return jobResult;
  }

  private String getConfigMapDataProp(V1ConfigMap configMap, String key) {
    String configMapDataProp = "";

    if (configMap.getData().get(key) != null) {
      configMapDataProp = configMap.getData().get(key);
      LOGGER.info(" ConfigMap Input Properties Data: " + configMapDataProp);
    }
    return configMapDataProp;
  }

  private void patchConfigMap(String name, String dataKey, String origData, String newData) {
    JsonObject jsonPatchObj = new JsonObject();
    jsonPatchObj.addProperty("op", "add");
    jsonPatchObj.addProperty("path", "/data/" + dataKey);
    jsonPatchObj.addProperty("value",
        origData.endsWith("\n") ? (origData + newData) : (origData + "\n" + newData));

    ArrayList<JsonObject> arr = new ArrayList<>();
    arr.add(((new Gson()).fromJson(jsonPatchObj.toString(), JsonElement.class)).getAsJsonObject());
    try {
      V1ConfigMap result =
          getCoreApi().patchNamespacedConfigMap(name, kubeNamespace, arr, kubeApiPretty, null);
      LOGGER.info(result);
    } catch (ApiException e) {
      LOGGER.error("Exception when calling CoreV1Api#patchNamespacedConfigMap", e);
    }
  }

  private StreamingResponseBody getPodLog(InputStream inputStream, String podName) {
    return outputStream -> {
      byte[] data = new byte[BYTE_SIZE];
      int nRead = 0;
      int nReadSum = 0;
      LOGGER.info("Log stream started for pod " + podName + "...");
      try {
        while ((nRead = inputStream.read(data)) > 0) {
          outputStream.write(data, 0, nRead);
          nReadSum += nRead;
        }
      } finally {
        outputStream.flush();
        LOGGER.info("Log stream completed for pod " + podName + ", total bytes streamed=" + nReadSum
            + "...");
        inputStream.close();
        LOGGER.info("Log stream closed for pod " + podName + "...");
      }
    };
  }

  private V1Pod getPod(Watch<V1Pod> watch) throws IOException {
    V1Pod pod = null;
    try {
      for (Watch.Response<V1Pod> item : watch) {

        String name = item.object.getMetadata().getName();
        LOGGER.info("Pod: " + name + "...");
        LOGGER.info("Pod Start Time: " + item.object.getStatus().getStartTime() + "...");
        String phase = item.object.getStatus().getPhase();
        LOGGER.info("Pod Phase: " + phase + "...");
        for (V1PodCondition condition : item.object.getStatus().getConditions()) {
          LOGGER.info("Pod Condition: " + condition.toString() + "...");
        }
        for (V1ContainerStatus containerStatus : item.object.getStatus().getContainerStatuses()) {
          LOGGER.info("Container Status: " + containerStatus.toString() + "...");
        }

        if (!("pending".equalsIgnoreCase(phase) || "unknown".equalsIgnoreCase(phase))) {
          LOGGER.info("Pod " + name + " ready to stream logs...");
          pod = item.object;
          break;
        }
      }
    } finally {
      watch.close();
    }
    return pod;
  }

  private StreamingResponseBody streamLogsFromElastic(String activityId) {
    LOGGER.info("Streaming logs from elastic: " + getPrefixJob() + "-" + activityId + "-*");

    return outputStream -> {
      PrintWriter printWriter = new PrintWriter(outputStream);

      final Scroll scroll = new Scroll(TimeValue.timeValueMinutes(1L));

      SearchRequest searchRequest = new SearchRequest("logstash-*");

      searchRequest.scroll(scroll);
      SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
      searchSourceBuilder.from(0);
      searchSourceBuilder.size(1000);
      searchSourceBuilder.sort("offset");
      searchSourceBuilder.query(QueryBuilders.matchPhraseQuery("kubernetes.pod",
    		  getPrefixJob() + "-" + activityId + "-*"));
      searchRequest.source(searchSourceBuilder);

      SearchResponse searchResponse = elasticRestClient.search(searchRequest);
      SearchHit[] searchHits = searchResponse.getHits().getHits();
      LOGGER.info("Search returned back: " + searchHits.length);

      if (searchHits.length == 0) {
        printWriter.println(getErrorMessage());
        printWriter.flush();
        printWriter.close();
        return;
      }

      for (SearchHit hits : searchHits) {
        String logMessage = (String) hits.getSourceAsMap().get("log");
        printWriter.println(logMessage);
      }

      String scrollId = searchResponse.getScrollId();
      while (searchHits != null && searchHits.length > 0) {
        SearchScrollRequest scrollRequest = new SearchScrollRequest(scrollId);
        scrollRequest.scroll(scroll);
        searchResponse = elasticRestClient.searchScroll(scrollRequest);
        scrollId = searchResponse.getScrollId();
        searchHits = searchResponse.getHits().getHits();
        LOGGER.info("Search returned back: " + searchHits.length);
        for (SearchHit hits : searchHits) {
          String logMessage = (String) hits.getSourceAsMap().get("log");
          printWriter.println(logMessage);
        }
      }

      ClearScrollRequest clearScrollRequest = new ClearScrollRequest();
      clearScrollRequest.addScrollId(scrollId);
      elasticRestClient.clearScroll(clearScrollRequest);

      printWriter.flush();
      printWriter.close();
    };
  }

  private StreamingResponseBody getExternalLogs(String activityId) {
      return streamLogsFromElastic(activityId);
  }

  private StreamingResponseBody getDefaultErrorMessage() {
    LOGGER.info("Returning back default message.");

    return outputStream -> {
      outputStream.write(getErrorMessage().getBytes(StandardCharsets.UTF_8));
      outputStream.flush();
      outputStream.close();
    };
  }

  private String getErrorMessage() {
    MessageSourceAccessor accessor = new MessageSourceAccessor(messageSource);
    return accessor.getMessage("logs.error");
  }

  private CoreV1Api getCoreApi() {
    return apiClient == null ? new CoreV1Api() : new CoreV1Api(apiClient);
  }

  private BatchV1Api getBatchApi() {
    return apiClient == null ? new BatchV1Api() : new BatchV1Api(apiClient);
  }

  void setApiClient(ApiClient apiClient) {
    this.apiClient = apiClient;
  }
}
