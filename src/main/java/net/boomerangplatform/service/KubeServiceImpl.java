package net.boomerangplatform.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import io.kubernetes.client.ApiException;
import io.kubernetes.client.apis.BatchV1Api;
import io.kubernetes.client.apis.CoreV1Api;
import io.kubernetes.client.apis.CustomObjectsApi;
import io.kubernetes.client.models.V1Container;
import io.kubernetes.client.models.V1Job;
import io.kubernetes.client.models.V1JobList;
import io.kubernetes.client.models.V1JobSpec;
import io.kubernetes.client.models.V1NamespaceList;
import io.kubernetes.client.models.V1ObjectMeta;
import io.kubernetes.client.models.V1PodSpec;
import io.kubernetes.client.models.V1PodTemplateSpec;
import net.boomerangplatform.model.Workflow;

@Service
public class KubeServiceImpl implements KubeService {

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
    	
    	String namespace = "default"; // String | object name and auth scope, such as for teams and projects
    	String pretty = "true"; // String | If 'true', then the output is pretty printed.
    	String _continue = ""; // String | The continue option should be set when retrieving more results from the server. Since this value is server defined, clients may only use the continue value from a previous query result with identical query parameters (except for the value of continue) and the server may reject a continue value it does not recognize. If the specified continue value is no longer valid whether due to expiration (generally five to fifteen minutes) or a configuration change on the server the server will respond with a 410 ResourceExpired error indicating the client must restart their list without the continue field. This field is not supported when watch is true. Clients may start a watch from the last resourceVersion value returned by the server and not miss any modifications.
    	String fieldSelector = ""; // String | A selector to restrict the list of returned objects by their fields. Defaults to everything.
    	Boolean includeUninitialized = false; // Boolean | If true, partially initialized resources are included in the response.
    	String labelSelector = ""; // String | A selector to restrict the list of returned objects by their labels. Defaults to everything.
    	Integer limit = 25; // Integer | limit is a maximum number of responses to return for a list call. If more items exist, the server will set the `continue` field on the list metadata to a value that can be used with the same initial query to retrieve the next set of results. Setting a limit may return fewer than the requested amount of items (up to zero items) in the event all requested objects are filtered out and clients should only use the presence of the continue field to determine whether more results are available. Servers may choose not to support the limit argument and will return all of the available results. If limit is specified and the continue field is empty, clients may assume that no more results are available. This field is not supported if watch is true.  The server guarantees that the objects returned when using continue will be identical to issuing a single list call without a limit - that is, no objects created, modified, or deleted after the first request is issued will be included in any subsequent continued requests. This is sometimes referred to as a consistent snapshot, and ensures that a client that is using limit to receive smaller chunks of a very large result can ensure they see all possible objects. If objects are updated during a chunked list the version of the object that was present at the time the first list result was calculated is returned.
    	String resourceVersion = ""; // String | When specified with a watch call, shows changes that occur after that particular version of a resource. Defaults to changes from the beginning of history. When specified for list: - if unset, then the result is returned from remote storage based on quorum-read flag; - if it's 0, then we simply return what we currently have in cache, no guarantee; - if set to non zero, then the result is at least as fresh as given rv.
    	Integer timeoutSeconds = 60; // Integer | Timeout for the list/watch call. This limits the duration of the call, regardless of any activity or inactivity.
    	Boolean watch = false; // Boolean | Watch for changes to the described resources and return them as a stream of add, update, and remove notifications. Specify resourceVersion.
    	
        try {
        	BatchV1Api api = new BatchV1Api();
        	list = api.listNamespacedJob(namespace, pretty, _continue, fieldSelector, includeUninitialized, labelSelector, limit, resourceVersion, timeoutSeconds, watch);

		} catch (ApiException e) {
			e.printStackTrace();
		}        
        return list;
    }
    
    @Override
    public V1Job createJob() {
    	V1Job jobResult = new V1Job();
    	
    	//Create Job
    	String namespace = "default"; // String | object name and auth scope, such as for teams and projects
    	V1Job body = new V1Job(); // V1Job | 
    	String pretty = "true"; // String | If 'true', then the output is pretty printed.
    	
    	//Create Metadata
    	V1ObjectMeta metadata = new V1ObjectMeta();
    	Map <String, String> annotations = new HashMap<String, String>();
    	annotations.put("boomerangplatform.net/workflow", "workflow-name");
    	metadata.annotations(annotations);
    	metadata.generateName("bmrg-flow-");
    	body.metadata(metadata);
    	
    	//Create Spec
    	V1JobSpec jobSpec = new V1JobSpec();
    	V1PodTemplateSpec templateSpec = new V1PodTemplateSpec();
    	V1PodSpec podSpec = new V1PodSpec();
    	V1Container container = new V1Container();
    	container.image("tools.boomerangplatform.net:8500/ise/bmrg-worker-flow:0.0.1");
    	container.name("bmrg-flow-cntr-worker");
    	List<V1Container> containerList = new ArrayList<V1Container>();
    	containerList.add(container);
    	podSpec.containers(containerList);
    	podSpec.restartPolicy("Never");
    	templateSpec.spec(podSpec);
    	jobSpec.template(templateSpec);
    	body.spec(jobSpec);

        try {
        	BatchV1Api api = new BatchV1Api();
        	jobResult = api.createNamespacedJob(namespace, body, pretty);

		} catch (ApiException e) {
			e.printStackTrace();
		}        
        return jobResult;
    }
    
    @Override
    public Workflow getWorkflow(String name) {
    	CustomObjectsApi api = new CustomObjectsApi();
    	Workflow response = null;
    	try {
    		response = (Workflow) api.getNamespacedCustomObject("argoproj.io", "v1alpha1", "default", "workflows", name);
    	}
    	catch (ApiException e) {
    		e.printStackTrace();
    	}    	
    	return response;
    }
    
    @Override
    public Object createWorkflow(Workflow workflow) {    	
    	CustomObjectsApi api = new CustomObjectsApi();
    	Object response = null;
    	try {
    		response = (Object) api.createNamespacedCustomObject("argoproj.io", "v1alpha1", "default", "workflows", workflow, null);
    	}
    	catch (ApiException e) {
    		e.printStackTrace();
    	}
    	return response;
    }
}
