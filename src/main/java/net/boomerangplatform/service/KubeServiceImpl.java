package net.boomerangplatform.service;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;

import io.kubernetes.client.ApiClient;
import io.kubernetes.client.ApiException;
import io.kubernetes.client.Configuration;
import io.kubernetes.client.apis.BatchV1Api;
import io.kubernetes.client.apis.CoreV1Api;
import io.kubernetes.client.auth.ApiKeyAuth;
import io.kubernetes.client.custom.Quantity;
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
import io.kubernetes.client.models.V1PodSpec;
import io.kubernetes.client.models.V1PodTemplateSpec;
import io.kubernetes.client.models.V1ResourceRequirements;
import io.kubernetes.client.models.V1Status;
import io.kubernetes.client.models.V1Volume;
import io.kubernetes.client.models.V1VolumeMount;
import io.kubernetes.client.util.Config;
import io.kubernetes.client.util.Watch;

@Service
public class KubeServiceImpl implements KubeService {
	
	@Value("${kube.api.base.path}")
	public String kubeApiBasePath;

	@Value("${kube.api.token}")
	public String kubeApiToken;
	
	@Value("${kube.api.debug}")
	public String kubeApiDebug;
	
	@Value("${kube.api.type}")
	public String kubeApiType;

	@Value("${kube.namespace}")
	public String kubeNamespace;
	
	@Value("${kube.image}")
	public String kubeImage;
	
	@Value("${kube.image.pullPolicy}")
	public String kubeImagePullPolicy;
	
	@Value("${proxy.enable}")
	public Boolean proxyEnabled;
	
	@Value("${proxy.host}")
	public String proxyHost;
	
	@Value("${proxy.port}")
	public String proxyPort;
	
	@Value("${proxy.ignore}")
	public String proxyIgnore;
	
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

	@Override
	public V1JobList getAllJobs() {
		V1JobList list = new V1JobList();

		String namespace = kubeNamespace; // String | object name and auth scope, such as for teams and projects
		String pretty = "true"; // String | If 'true', then the output is pretty printed.
		String _continue = ""; // String | The continue option should be set when retrieving more results from
								// the server. Since this value is server defined, clients may only use the
								// continue value from a previous query result with identical query parameters
								// (except for the value of continue) and the server may reject a continue value
								// it does not recognize. If the specified continue value is no longer valid
								// whether due to expiration (generally five to fifteen minutes) or a
								// configuration change on the server the server will respond with a 410
								// ResourceExpired error indicating the client must restart their list without
								// the continue field. This field is not supported when watch is true. Clients
								// may start a watch from the last resourceVersion value returned by the server
								// and not miss any modifications.
		String fieldSelector = ""; // String | A selector to restrict the list of returned objects by their fields.
									// Defaults to everything.
		Boolean includeUninitialized = false; // Boolean | If true, partially initialized resources are included in the
												// response.
		String labelSelector = ""; // String | A selector to restrict the list of returned objects by their labels.
									// Defaults to everything.
		Integer limit = 25; // Integer | limit is a maximum number of responses to return for a list call.
							// If more items exist, the server will set the `continue` field on the list
							// metadata to a value that can be used with the same initial query to retrieve
							// the next set of results. Setting a limit may return fewer than the requested
							// amount of items (up to zero items) in the event all requested objects are
							// filtered out and clients should only use the presence of the continue field
							// to determine whether more results are available. Servers may choose not to
							// support the limit argument and will return all of the available results. If
							// limit is specified and the continue field is empty, clients may assume that
							// no more results are available. This field is not supported if watch is true.
							// The server guarantees that the objects returned when using continue will be
							// identical to issuing a single list call without a limit - that is, no objects
							// created, modified, or deleted after the first request is issued will be
							// included in any subsequent continued requests. This is sometimes referred to
							// as a consistent snapshot, and ensures that a client that is using limit to
							// receive smaller chunks of a very large result can ensure they see all
							// possible objects. If objects are updated during a chunked list the version of
							// the object that was present at the time the first list result was calculated
							// is returned.
		String resourceVersion = ""; // String | When specified with a watch call, shows changes that occur after
										// that particular version of a resource. Defaults to changes from the beginning
										// of history. When specified for list: - if unset, then the result is returned
										// from remote storage based on quorum-read flag; - if it's 0, then we simply
										// return what we currently have in cache, no guarantee; - if set to non zero,
										// then the result is at least as fresh as given rv.
		Integer timeoutSeconds = 60; // Integer | Timeout for the list/watch call. This limits the duration of the
										// call, regardless of any activity or inactivity.
		Boolean watch = false; // Boolean | Watch for changes to the described resources and return them as a
								// stream of add, update, and remove notifications. Specify resourceVersion.

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
	public V1Job createJob(String workflowName, String workflowId, String workflowActivityId, String taskId, List<String> arguments, Map<String, String> inputProperties) {
		V1Job jobResult = new V1Job();

		// Create Job
		String namespace = kubeNamespace; // String | object name and auth scope, such as for teams and projects
		V1Job body = new V1Job(); // V1Job |
		String pretty = "true"; // String | If 'true', then the output is pretty printed.

		// Create Metadata
		V1ObjectMeta metadata = new V1ObjectMeta();
		Map<String, String> annotations = new HashMap<String, String>();
		annotations.put("boomerangplatform.net/org", "bmrg");
		annotations.put("boomerangplatform.net/app", "bmrg-flow");
		annotations.put("boomerangplatform.net/workflow-name", workflowName);
		annotations.put("boomerangplatform.net/workflow-id", workflowId);
		annotations.put("boomerangplatform.net/workflow-activity-id", workflowActivityId);
		annotations.put("boomerangplatform.net/task-id", taskId);
		metadata.annotations(annotations);
		Map<String, String> labels = new HashMap<String, String>();
		labels.put("org", "bmrg");
		labels.put("app", "bmrg-flow");
		labels.put("workflow-name", workflowName);
		labels.put("workflow-id", workflowId);
		labels.put("workflow-activity-id", workflowActivityId);
		labels.put("task-id", taskId);
		metadata.labels(labels);
		metadata.generateName("bmrg-flow-");
//		metadata.name("bmrg-flow-"+taskId);
		body.metadata(metadata);

		// Create Spec
		V1JobSpec jobSpec = new V1JobSpec();
		V1PodTemplateSpec templateSpec = new V1PodTemplateSpec();
		V1PodSpec podSpec = new V1PodSpec();
		V1Container container = new V1Container();
		container.image(kubeImage);
		container.name("bmrg-flow-worker-cntr");
		container.imagePullPolicy(kubeImagePullPolicy);
		List<V1EnvVar> envVars = new ArrayList<V1EnvVar>();
		inputProperties.forEach((key, value) -> {
			envVars.add(createEnvVar("INPUTS_PROPS_"+key.replace("-", "_").replace(".", "_").toUpperCase(), value));
		});
		if (proxyEnabled) {
			envVars.addAll(createProxyEnvVars());
		}
		container.env(envVars);
		container.args(arguments);
		if (!getPVCName(workflowId, workflowActivityId).isEmpty()) {
			V1VolumeMount volMount = new V1VolumeMount();
			volMount.name("bmrg-flow-vol-" + workflowActivityId);
			volMount.mountPath("/data");
			container.addVolumeMountsItem(volMount);
			List<V1Volume> volumesList = new ArrayList<V1Volume>();
			V1Volume workerVolume = new V1Volume();
			workerVolume.name("bmrg-flow-vol-" + workflowActivityId);
			V1PersistentVolumeClaimVolumeSource workerVolumePVCSource = new V1PersistentVolumeClaimVolumeSource();
			workerVolume.persistentVolumeClaim(workerVolumePVCSource.claimName(getPVCName(workflowId, workflowActivityId)));
			volumesList.add(workerVolume);
			podSpec.volumes(volumesList);
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
		jobSpec.template(templateSpec);
		body.spec(jobSpec);

		try {
			BatchV1Api api = new BatchV1Api();
			jobResult = api.createNamespacedJob(namespace, body, pretty);

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
            }
		}
		
		return jobResult;
	}
	
	@Override
	public String watchJob(String workflowId, String workflowActivityId, String taskId) throws ApiException, IOException {
		System.out.println("----- Start Watcher -----");
		
		BatchV1Api api = new BatchV1Api();

		Watch<V1Job> watch = Watch.createWatch(
				createWatcherApiClient(), api.listNamespacedJobCall(kubeNamespace, "true", null, null, null,
						"org=bmrg,app=bmrg-flow,workflow-id="+workflowId+",workflow-activity-id="+workflowActivityId+",task-id="+taskId, null, null, null, true, null, null),
				new TypeToken<Watch.Response<V1Job>>() {
				}.getType());
		String result = "failure";
		try {
			for (Watch.Response<V1Job> item : watch) {
				System.out.println(item.type + " : " + item.object.getMetadata().getName());
				System.out.println(item.object.getStatus());
				if (item.object.getStatus().getSucceeded() != null && item.object.getStatus().getSucceeded() == 1) {
					result = "success";
					break;
				}
			}
		} finally {
			watch.close();
		}
		System.out.println("----- End Watcher -----");
		return result;
	}
	
	public V1PersistentVolumeClaim createPVC(String workflowName, String workflowId, String workflowActivityId) {
		
		// Setup	
		CoreV1Api api = new CoreV1Api();
		String namespace = kubeNamespace; // String | object name and auth scope, such as for teams and projects
		String pretty = "true"; // String | If 'true', then the output is pretty printed.
		V1PersistentVolumeClaim body = new V1PersistentVolumeClaim(); // V1PersistentVolumeClaim |

		// Create Metadata
		V1ObjectMeta metadata = new V1ObjectMeta();
		Map<String, String> annotations = new HashMap<String, String>();
		annotations.put("boomerangplatform.net/org", "bmrg");
		annotations.put("boomerangplatform.net/app", "bmrg-flow");
		annotations.put("boomerangplatform.net/workflow-name", workflowName);
		annotations.put("boomerangplatform.net/workflow-id", workflowId);
		annotations.put("boomerangplatform.net/workflow-activity-id", workflowActivityId);
		metadata.annotations(annotations);
		Map<String, String> labels = new HashMap<String, String>();
		labels.put("org", "bmrg");
		labels.put("app", "bmrg-flow");
		labels.put("workflow-name", workflowName);
		labels.put("workflow-id", workflowId);
		labels.put("workflow-activity-id", workflowActivityId);
		metadata.labels(labels);
		metadata.generateName("bmrg-flow-pvc-");
		body.metadata(metadata);

		// Create PVC Spec
		V1PersistentVolumeClaimSpec pvcSpec = new V1PersistentVolumeClaimSpec();
		List<String> pvcAccessModes = new ArrayList<String>();
		pvcAccessModes.add("ReadWriteMany");
		pvcSpec.accessModes(pvcAccessModes);
		V1ResourceRequirements pvcResourceReq = new V1ResourceRequirements();
		Map<String, Quantity> pvcRequests = new HashMap<String, Quantity>();
		pvcRequests.put("storage", Quantity.fromString("1Gi"));
		pvcResourceReq.requests(pvcRequests);
		pvcSpec.resources(pvcResourceReq);
		body.spec(pvcSpec);
		
		V1PersistentVolumeClaim result = new V1PersistentVolumeClaim();
		try {
		    result = api.createNamespacedPersistentVolumeClaim(namespace, body, pretty);
		    System.out.println(result);
		    return result;
		} catch (ApiException e) {
		    System.err.println("Exception when calling CoreV1Api#createNamespacedPersistentVolumeClaim");
		    e.printStackTrace();
		}
		return null;
	}
	
	@Override
	public V1PersistentVolumeClaimStatus watchPVC(String workflowId, String workflowActivityId) throws ApiException, IOException {
		System.out.println("----- Start Watcher -----");
		
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
		System.out.println("----- End Watcher -----");
		return result;
	}
	
	@Override
	public V1Status deletePVC(String workflowId, String workflowActivityId) {
		System.out.println("----- Start deletePVC() -----");
		CoreV1Api api = new CoreV1Api();
		V1DeleteOptions pvcDeleteOptions = new V1DeleteOptions();
		V1Status result = new V1Status();
		String namespace = kubeNamespace; // String | object name and auth scope, such as for teams and projects
		String pretty = "true"; // String | If 'true', then the output is pretty printed.
		
		try {
			result = api.deleteNamespacedPersistentVolumeClaim(getPVCName(workflowId, workflowActivityId), namespace, pvcDeleteOptions, pretty, null, null, null);
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
		System.out.println("----- Start getPVCName() -----");
		
		CoreV1Api api = new CoreV1Api();
		String namespace = kubeNamespace; // String | object name and auth scope, such as for teams and projects
		String pretty = "true"; // String | If 'true', then the output is pretty printed.
		
		try {
			V1PersistentVolumeClaimList persistentVolumeClaimList = api.listNamespacedPersistentVolumeClaim(namespace, pretty, null, null, null, "org=bmrg,app=bmrg-flow,workflow-id="+workflowId+",workflow-activity-id="+workflowActivityId, null, null, 60, false);
			persistentVolumeClaimList.getItems().forEach(pvc -> {
				System.out.println(pvc.toString());
				System.out.println(" PVC Name: " + pvc.getMetadata().getName());
			});
			return persistentVolumeClaimList.getItems().get(0).getMetadata().getName();
		} catch (ApiException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return "";
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
