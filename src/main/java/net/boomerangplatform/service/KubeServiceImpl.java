package net.boomerangplatform.service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import com.google.common.io.ByteStreams;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;

import io.kubernetes.client.ApiClient;
import io.kubernetes.client.ApiException;
import io.kubernetes.client.Configuration;
import io.kubernetes.client.PodLogs;
import io.kubernetes.client.apis.BatchV1Api;
import io.kubernetes.client.apis.CoreV1Api;
import io.kubernetes.client.auth.ApiKeyAuth;
import io.kubernetes.client.custom.Quantity;
import io.kubernetes.client.models.V1ConfigMap;
import io.kubernetes.client.models.V1ConfigMapList;
import io.kubernetes.client.models.V1ConfigMapProjection;
import io.kubernetes.client.models.V1Container;
import io.kubernetes.client.models.V1DeleteOptions;
import io.kubernetes.client.models.V1EnvVar;
import io.kubernetes.client.models.V1Job;
import io.kubernetes.client.models.V1JobList;
import io.kubernetes.client.models.V1JobSpec;
import io.kubernetes.client.models.V1JobStatus;
import io.kubernetes.client.models.V1LocalObjectReference;
import io.kubernetes.client.models.V1Namespace;
import io.kubernetes.client.models.V1NamespaceList;
import io.kubernetes.client.models.V1ObjectMeta;
import io.kubernetes.client.models.V1PersistentVolumeClaim;
import io.kubernetes.client.models.V1PersistentVolumeClaimList;
import io.kubernetes.client.models.V1PersistentVolumeClaimSpec;
import io.kubernetes.client.models.V1PersistentVolumeClaimStatus;
import io.kubernetes.client.models.V1PersistentVolumeClaimVolumeSource;
import io.kubernetes.client.models.V1Pod;
import io.kubernetes.client.models.V1PodList;
import io.kubernetes.client.models.V1PodSpec;
import io.kubernetes.client.models.V1PodTemplateSpec;
import io.kubernetes.client.models.V1ProjectedVolumeSource;
import io.kubernetes.client.models.V1ResourceRequirements;
import io.kubernetes.client.models.V1Status;
import io.kubernetes.client.models.V1Volume;
import io.kubernetes.client.models.V1VolumeMount;
import io.kubernetes.client.models.V1VolumeProjection;
import io.kubernetes.client.util.Config;
import io.kubernetes.client.util.Watch;

@Service
public class KubeServiceImpl implements KubeService {
	
	@Value("${kube.api.base.path}")
	private String kubeApiBasePath;

	@Value("${kube.api.token}")
	private String kubeApiToken;
	
	@Value("${kube.api.debug}")
	private String kubeApiDebug;
	
	@Value("${kube.api.type}")
	private String kubeApiType;

	@Value("${kube.namespace}")
	private String kubeNamespace;
	
	@Value("${kube.image}")
	private String kubeImage;
	
	@Value("${kube.image.pullPolicy}")
	private String kubeImagePullPolicy;
	
	@Value("${kube.worker.pvc.initialSize}")
	private String kubeWorkerPVCInitialSize;
	
	@Value("${kube.worker.job.faillimit}")
	private Integer kubeWorkerJobFailLimit;
	
	@Value("${proxy.enable}")
	private Boolean proxyEnabled;
	
	@Value("${proxy.host}")
	private String proxyHost;
	
	@Value("${proxy.port}")
	private String proxyPort;
	
	@Value("${proxy.ignore}")
	private String proxyIgnore;
	
	@Value("${controller.service.host}")
	private String bmrgControllerServiceURL;
	
	final static String PREFIX_FLOW = "bmrg-flow-";
	
	final static String PREFIX_CFGMAP = PREFIX_FLOW + "cfg-";
	
	final static String PREFIX_PVC = PREFIX_FLOW + "pvc-";
	
	final static String PREFIX_VOL = PREFIX_FLOW + "vol-";
	
	final static String API_PRETTY = "true";
	
	//TODO most likely remove
	@Override
	public V1NamespaceList getAllNamespaces() {
		V1NamespaceList list = new V1NamespaceList();
		try {
			CoreV1Api api = new CoreV1Api();
			list = api.listNamespace(null, null, null, null, null, null, null, null, null);
		} catch (ApiException e) {
			e.printStackTrace();
		}
		return list;
	}

	//TODO update to return all the boomerang flow jobs and their status
	@Override
	public V1JobList getAllJobs() {
		V1JobList list = new V1JobList();

		String namespace = kubeNamespace;
		String pretty = "true";
		String _continue = "";
		String fieldSelector = "";
		Boolean includeUninitialized = false;
		String labelSelector = "";
		Integer limit = 25;
		String resourceVersion = "";
		Integer timeoutSeconds = 60;
		Boolean watch = false;

		try {
			BatchV1Api api = new BatchV1Api();
			list = api.listNamespacedJob(namespace, pretty, _continue, fieldSelector, includeUninitialized,
					labelSelector, limit, resourceVersion, timeoutSeconds, watch);

		} catch (ApiException e) {
			e.printStackTrace();
		}
		return list;
	}
	
	@Override
	public V1Job createJob(String workflowName, String workflowId, String workflowActivityId,String taskName, String taskId, List<String> arguments, Map<String, String> taskInputProperties) {

		// Set Variables
		final String volMountPath = "/data";
		final String cfgMapMountPath = "/props";

		// Initialize Job Body
		V1Job body = new V1Job(); // V1Job |
		
		// Create Metadata
		V1ObjectMeta jobMetadata = new V1ObjectMeta();
		jobMetadata.annotations(createAnnotations(workflowName, workflowId, workflowActivityId, taskId));
		jobMetadata.labels(createLabels(workflowName, workflowId, workflowActivityId, taskId));
		jobMetadata.generateName(PREFIX_FLOW);
//		metadata.name("bmrg-flow-"+taskId);
		body.metadata(jobMetadata);

		// Create Spec
		V1JobSpec jobSpec = new V1JobSpec();
		V1PodTemplateSpec templateSpec = new V1PodTemplateSpec();
		V1PodSpec podSpec = new V1PodSpec();
		V1Container container = new V1Container();
		container.image(kubeImage);
		container.name(PREFIX_FLOW + "worker-cntr");
		container.imagePullPolicy(kubeImagePullPolicy);
		List<V1EnvVar> envVars = new ArrayList<V1EnvVar>();
		//inputProperties.forEach((key, value) -> {
		//	envVars.add(createEnvVar("TASK_PROPS_"+key.replace("-", "_").replace(".", "_").toUpperCase(), value));
		//});
		//envVars.add(createEnvVar("OUTPUTS_PROPS_EXITCODE", "")); //to remove if configmap works
		if (proxyEnabled) {
			envVars.addAll(createProxyEnvVars());
		}
		container.env(envVars);
		container.args(arguments);
		if (!getPVCName(workflowId, workflowActivityId).isEmpty()) {
			V1VolumeMount volMount = new V1VolumeMount();
			volMount.name(PREFIX_VOL + "data");
			volMount.mountPath(volMountPath);
			container.addVolumeMountsItem(volMount);
			//List<V1Volume> volumesList = new ArrayList<V1Volume>();
			V1Volume workerVolume = new V1Volume();
			workerVolume.name(PREFIX_VOL + "data");
			V1PersistentVolumeClaimVolumeSource workerVolumePVCSource = new V1PersistentVolumeClaimVolumeSource();
			workerVolume.persistentVolumeClaim(workerVolumePVCSource.claimName(getPVCName(workflowId, workflowActivityId)));
			podSpec.addVolumesItem(workerVolume);
			//volumesList.add(workerVolume);
			//podSpec.volumes(volumesList);
		}
		V1ConfigMap wfConfigMap = getConfigMap(workflowId, workflowActivityId, taskId);
		if (wfConfigMap != null && !getConfigMapName(wfConfigMap).isEmpty()) {
			//Env Var Method
/*			V1ConfigMapEnvSource configMapEnvSource = new V1ConfigMapEnvSource();
			configMapEnvSource.name(getConfigMapName(workflowId, workflowActivityId));
			V1EnvFromSource envFromSource = new V1EnvFromSource();
			envFromSource.configMapRef(configMapEnvSource);
			envFromSource.prefix("WFINPUTS_PROPS_");
			List<V1EnvFromSource> envFromSourceList = new ArrayList<V1EnvFromSource>();
			envFromSourceList.add(envFromSource);
			container.envFrom(envFromSourceList);*/
			
			//Create Task ConfigMap Data
			//String fileName = taskName.replace(" ", "") + ".input.properties";
			//patchConfigMap(getConfigMapName(wfConfigMap), fileName, getConfigMapDataProp(wfConfigMap, fileName), createConfigMapProp(taskInputProperties));
			//Map<String, String> taskSysProperties =  new HashMap<String, String>();
			//taskSysProperties.put("id", taskId);
			//fileName = taskName.replace(" ", "") + ".system.properties";
			//patchConfigMap(getConfigMapName(wfConfigMap), fileName, getConfigMapDataProp(wfConfigMap, fileName), createConfigMapProp(taskSysProperties));
			
			//Container ConfigMap Mount
			V1VolumeMount volMountConfigMap = new V1VolumeMount();
			volMountConfigMap.name(PREFIX_VOL + "props");
			volMountConfigMap.mountPath(cfgMapMountPath);
			container.addVolumeMountsItem(volMountConfigMap);
			
			//Creation of Projected Volume with multiple ConfigMaps
			V1Volume volumeProps = new V1Volume();
			volumeProps.name(PREFIX_VOL + "props");
			V1ProjectedVolumeSource projectedVolPropsSource = new V1ProjectedVolumeSource();
			List<V1VolumeProjection> projectPropsVolumeList = new ArrayList<V1VolumeProjection>();
			
			V1ConfigMapProjection projectedConfigMapWorkflow = new V1ConfigMapProjection();
			projectedConfigMapWorkflow.name(PREFIX_CFGMAP + workflowActivityId);
			V1VolumeProjection configMapVolSourceWorkflow = new V1VolumeProjection();
			configMapVolSourceWorkflow.configMap(projectedConfigMapWorkflow);
			projectPropsVolumeList.add(configMapVolSourceWorkflow);
			
			V1ConfigMapProjection projectedConfigMapTask = new V1ConfigMapProjection();
			projectedConfigMapTask.name(PREFIX_CFGMAP + workflowActivityId + "-" + taskId);
			V1VolumeProjection configMapVolSourceTask = new V1VolumeProjection();
			configMapVolSourceTask.configMap(projectedConfigMapTask);
			projectPropsVolumeList.add(configMapVolSourceTask);
			
			projectedVolPropsSource.sources(projectPropsVolumeList);
			volumeProps.projected(projectedVolPropsSource);
			podSpec.addVolumesItem(volumeProps);
			
			//Workflow Configmap Mount --> Switched from ConfigMap volume to projected
			//V1ConfigMapVolumeSource configMapVolSourceWorkflow = new V1ConfigMapVolumeSource();
			//configMapVolSource.name(getConfigMapName(wfConfigMap)); //For now this is replaced with hardcoded string name
			//configMapVolSourceWorkflow.name(PREFIX_CFGMAP + workflowActivityId);
			//volumeConfigMapWorkflow.configMap(configMapVolSourceWorkflow);
			
			//Task ConfigMap Mount --> Switched from ConfigMap volume to projected
//			V1VolumeMount volMountTask = new V1VolumeMount();
//			volMountTask.name(PREFIX_PROPS + workflowActivityId + "-" + taskId);
//			volMountTask.mountPath(cfgMapMountPath);
//			container.addVolumeMountsItem(volMountTask);
//			V1Volume volumeConfigMapTask = new V1Volume();
//			volumeConfigMapTask.name(PREFIX_PROPS + workflowActivityId + "-" + taskId);
//			V1ConfigMapVolumeSource configMapVolSourceTask = new V1ConfigMapVolumeSource();
//			configMapVolSourceTask.name(PREFIX_CFGMAP + workflowActivityId + "-" + taskId);
//			List<V1KeyToPath> keyList = new ArrayList<V1KeyToPath>();
//			V1KeyToPath keyPath = new V1KeyToPath().key("task.input.properties").path("task.input.properties");
//			keyList.add(keyPath);
//			keyPath = new V1KeyToPath().key("task.system.properties").path("task.system.properties");
//			keyList.add(keyPath);
//			configMapVolSourceTask.items(keyList);
//			podSpec.addVolumesItem(volumeConfigMapTask);
		}
		List<V1Container> containerList = new ArrayList<V1Container>();
		containerList.add(container);
		podSpec.containers(containerList);
		V1LocalObjectReference imagePullSecret = new V1LocalObjectReference();
		imagePullSecret.name("boomerang.registrykey");
		List<V1LocalObjectReference> imagePullSecretList = new ArrayList<V1LocalObjectReference>();
		imagePullSecretList.add(imagePullSecret);
		podSpec.imagePullSecrets(imagePullSecretList);
		podSpec.restartPolicy("Never");
		templateSpec.spec(podSpec);
		
		//Pod metadata. Different to the job metadata
		V1ObjectMeta podMetadata = new V1ObjectMeta();
		podMetadata.annotations(createAnnotations(workflowName, workflowId, workflowActivityId, taskId));
		podMetadata.labels(createLabels(workflowName, workflowId, workflowActivityId, taskId));
		templateSpec.metadata(podMetadata);
		
		jobSpec.backoffLimit(1);
		jobSpec.template(templateSpec);
		body.spec(jobSpec);
		
		V1Job jobResult = new V1Job();
		try {
			BatchV1Api api = new BatchV1Api();
			jobResult = api.createNamespacedJob(kubeNamespace, body, API_PRETTY);
		} catch (ApiException e) {
			if (e.getCause() instanceof SocketTimeoutException) {
				SocketTimeoutException ste = (SocketTimeoutException) e.getCause();
                if (ste.getMessage() != null && ste.getMessage().contains("timeout")) {
                	System.out.println("Catching timeout and return as task error");
                	V1JobStatus badStatus = new V1JobStatus();
                	return body.status(badStatus.failed(1));
            	} else {
	                e.printStackTrace();
            	}
            } else {
                e.printStackTrace();
        	}
		}
		
		return jobResult;
	}
	
	@Override
	public String watchJob(String workflowId, String workflowActivityId, String taskId) throws Exception {		
		BatchV1Api api = new BatchV1Api();

		Watch<V1Job> watch = Watch.createWatch(
				createWatcherApiClient(), api.listNamespacedJobCall(kubeNamespace, "true", null, null, null,
						"org=bmrg,app=bmrg-flow,workflow-id="+workflowId+",workflow-activity-id="+workflowActivityId+",task-id="+taskId, null, null, null, true, null, null),
				new TypeToken<Watch.Response<V1Job>>() {
				}.getType());
		String result = "1";
		try {
			for (Watch.Response<V1Job> item : watch) {
				System.out.println(item.type + " : " + item.object.getMetadata().getName());
				System.out.println(item.object.getStatus());
				if (item.object.getStatus().getSucceeded() != null && item.object.getStatus().getSucceeded() == 1) {
					for (V1Container container : item.object.getSpec().getTemplate().getSpec().getContainers()) {
						System.out.println("Container Name: " + container.getName());
						System.out.println("Container Image: " + container.getImage());
					}
					result = "0";
					break;
				} else if (item.object.getStatus().getFailed() != null && item.object.getStatus().getFailed() >= kubeWorkerJobFailLimit) {
					//Implement manual check for failure as backOffLimit is not being respected in Kubernetes 1.10.4 and below
					throw new Exception("Task (" + taskId + ") has failed to execute " + kubeWorkerJobFailLimit + " times triggering failure");
				}
			}
		} finally {
			watch.close();
		}
		getJobPod(workflowId, workflowActivityId);
		return result;
	}
	
	//Does not work
	private void getJobPod(String workflowId, String workflowActivityId) {		
		CoreV1Api api = new CoreV1Api();
		String namespace = kubeNamespace; // String | object name and auth scope, such as for teams and projects
		String pretty = "true"; // String | If 'true', then the output is pretty printed.
		
		try {
			V1PodList podList = api.listNamespacedPod(namespace, pretty, null, null, null, "org=bmrg,app=bmrg-flow,workflow-id="+workflowId+",workflow-activity-id="+workflowActivityId, null, null, 60, false);
			podList.getItems().forEach(pod -> {
				System.out.println(pod.toString());
				System.out.println(" pod Name: " + pod.getMetadata().getName());
			});
		} catch (ApiException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	@Override
	public String getPodLog(String workflowId, String workflowActivityId, String taskId) throws ApiException, IOException {		
		CoreV1Api api = new CoreV1Api();
		String labelSelector = "org=bmrg,app=bmrg-flow,workflow-id="+workflowId+",workflow-activity-id="+workflowActivityId+",task-id=" + taskId;

	    PodLogs logs = new PodLogs();
	    List<V1Pod> listOfPods = 
	    		api
	            .listNamespacedPod(kubeNamespace, API_PRETTY, null, null, null, labelSelector, null, null, 60, false)
	            .getItems();
	    V1Pod pod = new V1Pod();
	    ByteArrayOutputStream baos = new ByteArrayOutputStream(); 
	    
	    if (!listOfPods.isEmpty()) {
	    	pod = listOfPods.get(0);
	    	InputStream is = logs.streamNamespacedPodLog(pod);
		    //ByteStreams.copy(is, System.out);
		    ByteStreams.copy(is, baos);
	    }
		return baos.toString();
	}
	
	@Override
	public StreamingResponseBody streamPodLog(HttpServletResponse response, String workflowId, String workflowActivityId, String taskId) throws ApiException, IOException {		
		CoreV1Api api = new CoreV1Api();
		String labelSelector = "org=bmrg,app=bmrg-flow,workflow-id="+workflowId+",workflow-activity-id="+workflowActivityId+",task-id=" + taskId;

	    PodLogs logs = new PodLogs();
	    V1Pod pod = 
	    		api
	            .listNamespacedPod(kubeNamespace, API_PRETTY, null, null, null, labelSelector, null, null, 60, false)
	            .getItems()
	            .get(0);

	    InputStream is = logs.streamNamespacedPodLog(pod);
	    ByteArrayOutputStream baos = new ByteArrayOutputStream(); 
	    //ByteStreams.copy(is, System.out);
	    ByteStreams.copy(is, baos);
		
		return outputStream -> {
		    int nRead;
		    byte[] data = new byte[1024];
		    while ((nRead = is.read(data, 0, data.length)) != -1) {
		        System.out.println("Writing some bytes of file...");
		        outputStream.write(data, 0, nRead);
		    }
		};
	}
	
	public V1PersistentVolumeClaim createPVC(String workflowName, String workflowId, String workflowActivityId, String pvcSize) throws ApiException {
		// Setup	
		CoreV1Api api = new CoreV1Api();
		String namespace = kubeNamespace; // String | object name and auth scope, such as for teams and projects
		String pretty = "true"; // String | If 'true', then the output is pretty printed.
		V1PersistentVolumeClaim body = new V1PersistentVolumeClaim(); // V1PersistentVolumeClaim |

		// Create Metadata
		V1ObjectMeta metadata = new V1ObjectMeta();
		metadata.annotations(createAnnotations(workflowName, workflowId, workflowActivityId, null));
		metadata.labels(createLabels(workflowName, workflowId, workflowActivityId, null));
		metadata.generateName(PREFIX_PVC);
		body.metadata(metadata);

		// Create PVC Spec
		V1PersistentVolumeClaimSpec pvcSpec = new V1PersistentVolumeClaimSpec();
		List<String> pvcAccessModes = new ArrayList<String>();
		pvcAccessModes.add("ReadWriteMany");
		pvcSpec.accessModes(pvcAccessModes);
		V1ResourceRequirements pvcResourceReq = new V1ResourceRequirements();
		Map<String, Quantity> pvcRequests = new HashMap<String, Quantity>();
		if (pvcSize == null || pvcSize.isEmpty()) {
			pvcSize = kubeWorkerPVCInitialSize;
		}
		pvcRequests.put("storage", Quantity.fromString(pvcSize));
		pvcResourceReq.requests(pvcRequests);
		pvcSpec.resources(pvcResourceReq);
		body.spec(pvcSpec);
		
		V1PersistentVolumeClaim result = new V1PersistentVolumeClaim();
	    result = api.createNamespacedPersistentVolumeClaim(namespace, body, pretty);
	    System.out.println(result);
	    return result;
	}
	
	@Override
	public V1PersistentVolumeClaimStatus watchPVC(String workflowId, String workflowActivityId) throws ApiException, IOException {
		CoreV1Api api = new CoreV1Api();
		
		Watch<V1PersistentVolumeClaim> watch = Watch.createWatch(
				createWatcherApiClient(), api.listNamespacedPersistentVolumeClaimCall(kubeNamespace, "true", null, null, null, "org=bmrg,app=bmrg-flow,workflow-id="+workflowId+",workflow-activity-id="+workflowActivityId, null, null, null, true, null, null),
				new TypeToken<Watch.Response<V1PersistentVolumeClaim>>() {
				}.getType());
		V1PersistentVolumeClaimStatus result = null;
		try {
			for (Watch.Response<V1PersistentVolumeClaim> item : watch) {
				System.out.printf("%s : %s%n", item.type, item.object.getMetadata().getName());
				System.out.println(item.object.getStatus());
				result = item.object.getStatus();
				if (result != null && result.getPhase() != null && result.getPhase().equals("Bound")) {
					break;
				}
			}
		} finally {
			watch.close();
		}
		return result;
	}
	
	@Override
	public V1Status deletePVC(String workflowId, String workflowActivityId) {
		CoreV1Api api = new CoreV1Api();
		V1DeleteOptions deleteOptions = new V1DeleteOptions();
		V1Status result = new V1Status();
		String namespace = kubeNamespace; // String | object name and auth scope, such as for teams and projects
		String pretty = "true"; // String | If 'true', then the output is pretty printed.
		
		try {
			result = api.deleteNamespacedPersistentVolumeClaim(getPVCName(workflowId, workflowActivityId), namespace, deleteOptions, pretty, null, null, null);
		} catch (JsonSyntaxException e) {
            if (e.getCause() instanceof IllegalStateException) {
                IllegalStateException ise = (IllegalStateException) e.getCause();
                if (ise.getMessage() != null && ise.getMessage().contains("Expected a string but was BEGIN_OBJECT")) {
                	System.out.println("Catching exception because of issue https://github.com/kubernetes-client/java/issues/86");
            	} else {
	                System.err.println("Exception when running deletePV()");
	    		    e.printStackTrace();
            	}
            }
        } catch (ApiException e) {
		    System.err.println("Exception when running deletePVC()");
		    e.printStackTrace();
		}
		return result;
	}
	
	private String getPVCName(String workflowId, String workflowActivityId) {
		CoreV1Api api = new CoreV1Api();
		String namespace = kubeNamespace; // String | object name and auth scope, such as for teams and projects
		String pretty = "true"; // String | If 'true', then the output is pretty printed.
		
		try {
			V1PersistentVolumeClaimList persistentVolumeClaimList = api.listNamespacedPersistentVolumeClaim(namespace, pretty, null, null, null, "org=bmrg,app=bmrg-flow,workflow-id="+workflowId+",workflow-activity-id="+workflowActivityId, null, null, 60, false);
			persistentVolumeClaimList.getItems().forEach(pvc -> {
				System.out.println(pvc.toString());
				System.out.println(" PVC Name: " + pvc.getMetadata().getName());
			});
			if (!persistentVolumeClaimList.getItems().isEmpty() && persistentVolumeClaimList.getItems().get(0).getMetadata().getName() != null) {
				System.out.println("----- End getPVCName() -----");
				return persistentVolumeClaimList.getItems().get(0).getMetadata().getName();
			}
		} catch (ApiException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return "";
	}

	@Override
	public V1ConfigMap createWorkflowConfigMap(String workflowName, String workflowId, String workflowActivityId, Map<String, String> inputProps) throws ApiException, IOException {
		V1ConfigMap body = new V1ConfigMap(); // V1PersistentVolumeClaim |
		
		// Create Metadata
		V1ObjectMeta metadata = new V1ObjectMeta();
		metadata.annotations(createAnnotations(workflowName, workflowId, workflowActivityId, null));
		metadata.labels(createLabels(workflowName, workflowId, workflowActivityId, null));
		//metadata.generateName(PREFIX_CFGMAP);
		metadata.name(PREFIX_CFGMAP + workflowActivityId); //We are using a fixed name to make it easier to find for subsequent calls as not all the configmap API's search on labels. Some take name as the parameter.
		body.metadata(metadata);
		
		//Create Data
		Map<String, String> inputsWithFixedKeys = new HashMap<String, String>();
		Map<String, String> sysProps = new HashMap<String, String>();
		sysProps.put("activity.id", workflowActivityId);
		sysProps.put("workflow.name", workflowName);
		sysProps.put("workflow.id", workflowId);
		sysProps.put("controller.service.url", bmrgControllerServiceURL);
		inputsWithFixedKeys.put("workflow.input.properties", createConfigMapProp(inputProps));
		inputsWithFixedKeys.put("workflow.system.properties", createConfigMapProp(sysProps));
		body.data(inputsWithFixedKeys);
		
		//Create ConfigMap
		V1ConfigMap result = createConfigMap(body);
		return result;
	}
	
	@Override
	public V1ConfigMap createTaskConfigMap(String workflowName, String workflowId, String workflowActivityId, String taskName, String taskId, Map<String, String> inputProps) throws ApiException, IOException {
		V1ConfigMap body = new V1ConfigMap(); // V1PersistentVolumeClaim |
		
		// Create Metadata
		V1ObjectMeta metadata = new V1ObjectMeta();
		metadata.annotations(createAnnotations(workflowName, workflowId, workflowActivityId, taskId));
		metadata.labels(createLabels(workflowName, workflowId, workflowActivityId, taskId));
		//metadata.generateName(PREFIX_CFGMAP);
		metadata.name(PREFIX_CFGMAP + workflowActivityId + "-" + taskId); //We are using a fixed name to make it easier to find for subsequent calls as not all the configmap API's search on labels. Some take name as the parameter.
		body.metadata(metadata);
		
		//Create Data
		Map<String, String> inputsWithFixedKeys = new HashMap<String, String>();
		Map<String, String> sysProps = new HashMap<String, String>();
		sysProps.put("task.id", taskId);
		sysProps.put("task.name", taskName);
		inputsWithFixedKeys.put("task.input.properties", createConfigMapProp(inputProps));
		inputsWithFixedKeys.put("task.system.properties", createConfigMapProp(sysProps));
		body.data(inputsWithFixedKeys);
		
		//Create ConfigMap
		V1ConfigMap result = createConfigMap(body);
		return result;
	}
	
	private V1ConfigMap createConfigMap(V1ConfigMap body) throws ApiException, IOException {
		CoreV1Api api = new CoreV1Api();
		
		//Create ConfigMap
		V1ConfigMap result = new V1ConfigMap();
		try {
		    result = api.createNamespacedConfigMap(kubeNamespace, body, API_PRETTY);
		    System.out.println(result);
		} catch (ApiException e) {
		    System.err.println("Exception when calling CoreV1Api#createNamespacedConfigMap");
		    e.printStackTrace();
		    throw e;
		}
		return result;
	}
	
	@Override
	public V1ConfigMap watchConfigMap(String workflowId, String workflowActivityId, String taskId) throws ApiException, IOException {
		CoreV1Api api = new CoreV1Api();
		String labelSelector = "org=bmrg,app=bmrg-flow,workflow-id="+workflowId+",workflow-activity-id="+workflowActivityId;
		if (taskId != null) {
			labelSelector = labelSelector.concat(",task-id=" + taskId);
		}
		System.out.println("  task-id: " + taskId);
		System.out.println("  labelSelector: " + labelSelector);
		
		Watch<V1ConfigMap> watch = Watch.createWatch(
				createWatcherApiClient(), api.listNamespacedConfigMapCall(kubeNamespace, API_PRETTY, null, null, null, labelSelector, null, null, null, true, null, null),
				new TypeToken<Watch.Response<V1ConfigMap>>() {
				}.getType());
		V1ConfigMap result = null;
		try {
			for (Watch.Response<V1ConfigMap> item : watch) {
				System.out.printf("%s : %s%n", item.type, item.object.getMetadata().getName());
				result = item.object;
				break;
			}
		} finally {
			watch.close();
		}
		return result;
	}
	
	private V1ConfigMap getConfigMap(String workflowId, String workflowActivityId, String taskId) {
		V1ConfigMap configMap = null;
		
		CoreV1Api api = new CoreV1Api();
		String labelSelector = "org=bmrg,app=bmrg-flow,workflow-id="+workflowId+",workflow-activity-id="+workflowActivityId;
		if (taskId != null) {
			labelSelector = labelSelector.concat(",task-id=" + taskId);
		}
		System.out.println("  task-id: " + taskId);
		System.out.println("  labelSelector: " + labelSelector);
		try {
			V1ConfigMapList configMapList = api.listNamespacedConfigMap(kubeNamespace, API_PRETTY, null, null, null, labelSelector, null, null, 60, false);
			configMapList.getItems().forEach(cfgmap -> {
				System.out.println(cfgmap.toString());
			});
			if (!configMapList.getItems().isEmpty()) {
				configMap = configMapList.getItems().get(0);
			}
		} catch (ApiException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return configMap;
	}
	
	private String getConfigMapName(V1ConfigMap configMap) {
		String configMapName = "";
		
		if (configMap != null && !configMap.getMetadata().getName().isEmpty()) {
			configMapName = configMap.getMetadata().getName();
			System.out.println(" ConfigMap Name: " + configMapName);
		}
		return configMapName;
	}
	
	private String getConfigMapDataProp(V1ConfigMap configMap, String key) {
		String configMapDataProp = "";
		
		if (configMap.getData().get(key) != null) {
			configMapDataProp = configMap.getData().get(key);
			System.out.println(" ConfigMap Input Properties Data: " + configMapDataProp);
		}
		return configMapDataProp;
	}
	
	private String patchConfigMap(String name, String dataKey, String origData, String newData) {
		CoreV1Api api = new CoreV1Api();
		String namespace = kubeNamespace; // String | object name and auth scope, such as for teams and projects
		String pretty = "true"; // String | If 'true', then the output is pretty printed.
		String combinedData = "";
		if (origData.endsWith("\n")) {
			combinedData = origData + newData;
		} else {
			combinedData = origData + "\n" + newData;
		}
		String jsonPatchStr = "{\"op\":\"add\",\"path\":\"/data/" + dataKey + "\",\"value\":\"" + combinedData + "\"}";
		ArrayList<JsonObject> arr = new ArrayList<>();
	    arr.add(((JsonElement) (new Gson()).fromJson(jsonPatchStr, JsonElement.class)).getAsJsonObject());
		try {
		    V1ConfigMap result = api.patchNamespacedConfigMap(name, namespace, arr, pretty);
		    System.out.println(result);
		    return "success"; // need to update with the status from result once its printed out and understood
		} catch (ApiException e) {
		    System.err.println("Exception when calling CoreV1Api#patchNamespacedConfigMap");
		    e.printStackTrace();
		}
		return "fail"; // need to update with the status from result once its printed out and understood
	}
	
	@Override
	public void patchTaskConfigMap(String workflowId, String workflowActivityId, String taskId, String taskName, Map<String, String> properties) {
		V1ConfigMap wfConfigMap = getConfigMap(workflowId, workflowActivityId, null);
		String fileName = taskName + ".output.properties";
		patchConfigMap(getConfigMapName(wfConfigMap), fileName, getConfigMapDataProp(wfConfigMap, fileName), createConfigMapProp(properties));
	}
	
	@Override
	public Map<String, String> getTaskOutPutConfigMapData(String workflowId, String workflowActivityId, String taskId, String taskName) {
		System.out.println("  taskName: " + taskName);
		Map<String, String> properties = new HashMap<String, String>();
		V1ConfigMap wfConfigMap = getConfigMap(workflowId, workflowActivityId, null);
		String fileName = taskName.replace(" ", "") + ".output.properties";
		String dataString = getConfigMapDataProp(wfConfigMap, fileName);
		
		properties = Pattern.compile("\\s*\\n\\s*")
			    .splitAsStream(dataString.trim())
			    .map(s -> s.split("=", 2))
			    .collect(Collectors.toMap(a -> a[0], a -> a.length>1? a[1]: ""));
		
		System.out.println("  properties: " + properties.toString());
		return properties;
	}
	
	@Override
	public V1Status deleteConfigMap(String workflowId, String workflowActivityId, String taskId) {
		CoreV1Api api = new CoreV1Api();
		V1DeleteOptions deleteOptions = new V1DeleteOptions();
		V1Status result = new V1Status();
		
		String name = PREFIX_CFGMAP + workflowActivityId;
		if (taskId != null) {
			name = name.concat("-" + taskId);
		}
		System.out.println("  Configmap to delete: " + name);
		
		try {
			//result = api.deleteNamespacedConfigMap(getConfigMapName(getConfigMap(workflowId, workflowActivityId, taskId)), kubeNamespace, deleteOptions, API_PRETTY, null, null, null);
			result = api.deleteNamespacedConfigMap(name, kubeNamespace, deleteOptions, API_PRETTY, null, null, null);
		} catch (JsonSyntaxException e) {
            if (e.getCause() instanceof IllegalStateException) {
                IllegalStateException ise = (IllegalStateException) e.getCause();
                if (ise.getMessage() != null && ise.getMessage().contains("Expected a string but was BEGIN_OBJECT")) {
                	System.out.println("Catching exception because of issue https://github.com/kubernetes-client/java/issues/86");
            	} else {
	                System.err.println("Exception when running deleteConfigMap()");
	    		    e.printStackTrace();
            	}
            }
        } catch (ApiException e) {
		    System.err.println("Exception when running deleteConfigMap()");
		    e.printStackTrace();
		}
		return result;
	}
	
	private ApiClient createWatcherApiClient() {
//		https://github.com/kubernetes-client/java/blob/master/util/src/main/java/io/kubernetes/client/util/Config.java#L57
//		ApiClient watcherClient = Config.fromToken(kubeApiBasePath, kubeApiToken, false);
		
		ApiClient watcherClient = io.kubernetes.client.Configuration.getDefaultApiClient().setVerifyingSsl(false).setDebugging(false);
		
		if (!kubeApiToken.isEmpty()) {
			ApiKeyAuth watcherApiKeyAuth = (ApiKeyAuth) watcherClient.getAuthentication("BearerToken");
			watcherApiKeyAuth.setApiKey(kubeApiToken);
			watcherApiKeyAuth.setApiKeyPrefix("Bearer");
		}
		watcherClient.getHttpClient().setReadTimeout(300, TimeUnit.SECONDS);
		return watcherClient;
	}
	
	private List<V1EnvVar> createProxyEnvVars() {
		List<V1EnvVar> proxyEnvVars = new ArrayList<V1EnvVar>();
		
		proxyEnvVars.add(createEnvVar("HTTP_PROXY","http://" + proxyHost + ":" + proxyPort));
		proxyEnvVars.add(createEnvVar("HTTPS_PROXY","https://" + proxyHost + ":" + proxyPort));
		proxyEnvVars.add(createEnvVar("http_proxy","http://" + proxyHost + ":" + proxyPort));
		proxyEnvVars.add(createEnvVar("https_proxy","https://" + proxyHost + ":" + proxyPort));
		proxyEnvVars.add(createEnvVar("NO_PROXY",proxyIgnore));
		proxyEnvVars.add(createEnvVar("no_proxy",proxyIgnore));
		proxyEnvVars.add(createEnvVar("use_proxy","on"));

		return proxyEnvVars;
	}
	
	private V1EnvVar createEnvVar(String key, String value) {
		V1EnvVar envVar = new V1EnvVar();
		envVar.setName(key);
		envVar.setValue(value);
		return envVar;
	}
	
	private Map<String, String> createAnnotations(String workflowName, String workflowId, String workflowActivityId, String taskId) {
		Map<String, String> annotations = new HashMap<String, String>();
		annotations.put("boomerangplatform.net/org", "bmrg");
		annotations.put("boomerangplatform.net/app", "bmrg-flow");
		annotations.put("boomerangplatform.net/workflow-name", workflowName);
		annotations.put("boomerangplatform.net/workflow-id", workflowId);
		annotations.put("boomerangplatform.net/workflow-activity-id", workflowActivityId);
		Optional.ofNullable(taskId).ifPresent(str -> annotations.put("boomerangplatform.net/task-id", str));
		
		return annotations;
	}
	
	private Map<String, String> createLabels(String workflowName, String workflowId, String workflowActivityId, String taskId) {
		Map<String, String> labels = new HashMap<String, String>();
		labels.put("org", "bmrg");
		labels.put("app", "bmrg-flow");
		labels.put("workflow-name", workflowName);
		labels.put("workflow-id", workflowId);
		labels.put("workflow-activity-id", workflowActivityId);
		Optional.ofNullable(taskId).ifPresent(str -> labels.put("task-id", str));
		
		return labels;
	}
	
	private String createConfigMapProp(Map<String, String> properties) {
		StringBuilder propsString = new StringBuilder();
		
		//TODO fix up null check and handling
		if (properties != null && !properties.isEmpty()) {
			properties.forEach((key, value) -> {
				//propsString.append(key.replace("-", "_").replace(".", "_").toUpperCase());
				propsString.append(key);
				propsString.append("=");
				propsString.append(value);
				propsString.append("\n");
			});
		} else {
			propsString.append("\n");
		}
		
		return propsString.toString();
	}
	
	private String createConfigMapPropWithPrefix(Map<String, String> properties, String prefix) {
		StringBuilder propsString = new StringBuilder();
		
		properties.forEach((key, value) -> {
			propsString.append(prefix + key.replace("-", "_").replace(".", "_").toUpperCase());
			propsString.append("=");
			propsString.append(value);
			propsString.append("\n");
		});
		
		return propsString.toString();
	}

	@Override
	public void watchNamespace() throws ApiException, IOException {
		
		ApiClient client = Config.defaultClient();
	    client.getHttpClient().setReadTimeout(60, TimeUnit.SECONDS);
	    Configuration.setDefaultApiClient(client);

		CoreV1Api api = new CoreV1Api();

		Watch<V1Namespace> watch = Watch.createWatch(client,
				api.listNamespaceCall(null, null, null, null, null, 120, null, null, Boolean.TRUE, null, null),
				new TypeToken<Watch.Response<V1Namespace>>() {
				}.getType());

		try {
			for (Watch.Response<V1Namespace> item : watch) {
				System.out.printf("%s : %s%n", item.type, item.object.getMetadata().getName());
			}
		} finally {
			watch.close();
		}
	}
}
