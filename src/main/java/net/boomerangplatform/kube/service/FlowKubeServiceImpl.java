package net.boomerangplatform.kube.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import io.kubernetes.client.models.V1ConfigMap;
import io.kubernetes.client.models.V1ConfigMapProjection;
import io.kubernetes.client.models.V1Container;
import io.kubernetes.client.models.V1EnvVar;
import io.kubernetes.client.models.V1Job;
import io.kubernetes.client.models.V1JobSpec;
import io.kubernetes.client.models.V1LocalObjectReference;
import io.kubernetes.client.models.V1ObjectMeta;
import io.kubernetes.client.models.V1PersistentVolumeClaimVolumeSource;
import io.kubernetes.client.models.V1PodSpec;
import io.kubernetes.client.models.V1PodTemplateSpec;
import io.kubernetes.client.models.V1ProjectedVolumeSource;
import io.kubernetes.client.models.V1Volume;
import io.kubernetes.client.models.V1VolumeMount;
import io.kubernetes.client.models.V1VolumeProjection;

@Service
@Profile("flow")
public class FlowKubeServiceImpl extends AbstractKubeServiceImpl {
	
	@Value("${kube.image}")
	private String kubeImage;
	
	final static String ORG = "bmrg";
	
	final static String PREFIX = ORG + "-flow";
	
	final static String PREFIX_CFGMAP = PREFIX + "-cfg";
	
	final static String PREFIX_PVC = PREFIX + "-pvc";
	
	final static String PREFIX_VOL = PREFIX + "-vol";
	
	final static String PREFIX_VOL_DATA = PREFIX_VOL + "-data";
	
	final static String PREFIX_VOL_PROPS = PREFIX_VOL + "-props";
	
	@Override
	protected V1Job createJobBody(String workflowName, String workflowId, String activityId,String taskName, String taskId, List<String> arguments, Map<String, String> taskInputProperties) {

		// Set Variables
		final String volMountPath = "/data";
		final String cfgMapMountPath = "/props";

		// Initialize Job Body
		V1Job body = new V1Job(); // V1Job |
		
		// Create Metadata
		V1ObjectMeta jobMetadata = new V1ObjectMeta();
		jobMetadata.annotations(createAnnotations(workflowName, workflowId, activityId, taskId));
		jobMetadata.labels(createLabels(workflowName, workflowId, activityId, taskId));
		jobMetadata.generateName(PREFIX + "-");
//		Uncomment if you want to hard code the name and watch on name rather than labels
//		metadata.name("bmrg-flow-"+taskId); 
		body.metadata(jobMetadata);

		// Create Spec
		V1JobSpec jobSpec = new V1JobSpec();
		V1PodTemplateSpec templateSpec = new V1PodTemplateSpec();
		V1PodSpec podSpec = new V1PodSpec();
		V1Container container = new V1Container();
		container.image(kubeImage);
		container.name("worker-cntr");
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
		if (!getPVCName(workflowId, activityId).isEmpty()) {
			V1VolumeMount volMount = new V1VolumeMount();
			volMount.name(PREFIX_VOL_DATA);
			volMount.mountPath(volMountPath);
			container.addVolumeMountsItem(volMount);
			V1Volume workerVolume = new V1Volume();
			workerVolume.name(PREFIX_VOL_DATA);
			V1PersistentVolumeClaimVolumeSource workerVolumePVCSource = new V1PersistentVolumeClaimVolumeSource();
			workerVolume.persistentVolumeClaim(workerVolumePVCSource.claimName(getPVCName(workflowId, activityId)));
			podSpec.addVolumesItem(workerVolume);
		}
		//Container ConfigMap Mount
		V1VolumeMount volMountConfigMap = new V1VolumeMount();
		volMountConfigMap.name(PREFIX_VOL_PROPS);
		volMountConfigMap.mountPath(cfgMapMountPath);
		container.addVolumeMountsItem(volMountConfigMap);
		
		//Creation of Projected Volume with multiple ConfigMaps
		V1Volume volumeProps = new V1Volume();
		volumeProps.name(PREFIX_VOL_PROPS);
		V1ProjectedVolumeSource projectedVolPropsSource = new V1ProjectedVolumeSource();
		List<V1VolumeProjection> projectPropsVolumeList = new ArrayList<V1VolumeProjection>();
		
		//Add Worfklow Configmap Projected Volume
		V1ConfigMap wfConfigMap = getConfigMap(workflowId, activityId, taskId);
		if (wfConfigMap != null && !getConfigMapName(wfConfigMap).isEmpty()) {
			V1ConfigMapProjection projectedConfigMapWorkflow = new V1ConfigMapProjection();
			projectedConfigMapWorkflow.name(getConfigMapName(wfConfigMap));
			V1VolumeProjection configMapVolSourceWorkflow = new V1VolumeProjection();
			configMapVolSourceWorkflow.configMap(projectedConfigMapWorkflow);
			projectPropsVolumeList.add(configMapVolSourceWorkflow);
		}

		//Add Task Configmap Projected Volume
		V1ConfigMap taskConfigMap = getConfigMap(workflowId, activityId, taskId);
		if (taskConfigMap != null && !getConfigMapName(taskConfigMap).isEmpty()) {
			V1ConfigMapProjection projectedConfigMapTask = new V1ConfigMapProjection();
			projectedConfigMapTask.name(getConfigMapName(taskConfigMap));
			V1VolumeProjection configMapVolSourceTask = new V1VolumeProjection();
			configMapVolSourceTask.configMap(projectedConfigMapTask);
			projectPropsVolumeList.add(configMapVolSourceTask);
		}
		
		//Add all configmap projected volume
		projectedVolPropsSource.sources(projectPropsVolumeList);
		volumeProps.projected(projectedVolPropsSource);
		podSpec.addVolumesItem(volumeProps);
		
		List<V1Container> containerList = new ArrayList<V1Container>();
		containerList.add(container);
		podSpec.containers(containerList);
		V1LocalObjectReference imagePullSecret = new V1LocalObjectReference();
		imagePullSecret.name(kubeImagePullSecret);
		List<V1LocalObjectReference> imagePullSecretList = new ArrayList<V1LocalObjectReference>();
		imagePullSecretList.add(imagePullSecret);
		podSpec.imagePullSecrets(imagePullSecretList);
		podSpec.restartPolicy(kubeWorkerJobRestartPolicy);
		templateSpec.spec(podSpec);
		
		//Pod metadata. Different to the job metadata
		V1ObjectMeta podMetadata = new V1ObjectMeta();
		podMetadata.annotations(createAnnotations(workflowName, workflowId, activityId, taskId));
		podMetadata.labels(createLabels(workflowName, workflowId, activityId, taskId));
		templateSpec.metadata(podMetadata);
		
		jobSpec.backoffLimit(kubeWorkerJobBackOffLimit);
		jobSpec.template(templateSpec);
		body.spec(jobSpec);
		
		return body;
	}
	
	protected V1ConfigMap createTaskConfigMapBody(
		      String workflowName,
		      String workflowId,
		      String workflowActivityId,
		      String taskName,
		      String taskId,
		      Map<String, String> inputProps) {
		    V1ConfigMap body = new V1ConfigMap(); // V1PersistentVolumeClaim |
		    
		    // Create Metadata
		    V1ObjectMeta metadata = new V1ObjectMeta();
		    metadata.annotations(createAnnotations(workflowName, workflowId, workflowActivityId, taskId));
		    metadata.labels(createLabels(workflowName, workflowId, workflowActivityId, taskId));
		    metadata.generateName(PREFIX_CFGMAP);
//				metadata.name(PREFIX_CFGMAP + workflowActivityId + "-" + taskId); //We are using a fixed name to make it easier to find for subsequent calls as not all the configmap API's search on labels. Some take name as the parameter.
		    body.metadata(metadata);
		    
		    //Create Data
		    Map<String, String> inputsWithFixedKeys = new HashMap<String, String>();
		    Map<String, String> sysProps = new HashMap<String, String>();
		    sysProps.put("task.id", taskId);
		    sysProps.put("task.name", taskName);
		    inputsWithFixedKeys.put("task.input.properties", createConfigMapProp(inputProps));
		    inputsWithFixedKeys.put("task.system.properties", createConfigMapProp(sysProps));
		    body.data(inputsWithFixedKeys);
		    return body;
		  }

	  protected V1ConfigMap createWorkflowConfigMapBody(
	      String workflowName,
	      String workflowId,
	      String workflowActivityId,
	      Map<String, String> inputProps) {
	    V1ConfigMap body = new V1ConfigMap(); // V1PersistentVolumeClaim |
	    
	    // Create Metadata
	    V1ObjectMeta metadata = new V1ObjectMeta();
	    metadata.annotations(createAnnotations(workflowName, workflowId, workflowActivityId, null));
	    metadata.labels(createLabels(workflowName, workflowId, workflowActivityId, null));
	    metadata.generateName(PREFIX_CFGMAP);
//			metadata.name(PREFIX_CFGMAP + workflowActivityId); //We are using a fixed name to make it easier to find for subsequent calls as not all the configmap API's search on labels. Some take name as the parameter.
	    body.metadata(metadata);
	    
	    //Create Data
	    Map<String, String> inputsWithFixedKeys = new HashMap<String, String>();
	    Map<String, String> sysProps = new HashMap<String, String>();
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
	    String labelSelector =
	        "org=" + ORG + ",app=" + PREFIX + ",workflow-id="
	            + workflowId
	            + ",activity-id="
	            + activityId;
	    return Optional.ofNullable(taskId).isPresent() ? labelSelector.concat(",task-id=" + taskId) : labelSelector;
	  }
	  
		protected Map<String, String> createAnnotations(String workflowName, String workflowId, String activityId, String taskId) {
			Map<String, String> annotations = new HashMap<String, String>();
			annotations.put("boomerangplatform.net/org", ORG);
			annotations.put("boomerangplatform.net/app", PREFIX);
			annotations.put("boomerangplatform.net/workflow-name", workflowName);
			annotations.put("boomerangplatform.net/workflow-id", workflowId);
			annotations.put("boomerangplatform.net/activity-id", activityId);
			Optional.ofNullable(taskId).ifPresent(str -> annotations.put("boomerangplatform.net/task-id", str));
			
			return annotations;
		}
		
		protected Map<String, String> createLabels(String workflowName, String workflowId, String activityId, String taskId) {
			Map<String, String> labels = new HashMap<String, String>();
			labels.put("org", ORG);
			labels.put("app", PREFIX);
			Optional.ofNullable(workflowName).ifPresent(str -> labels.put("workflow-name", str.replace(" ", "")));
			Optional.ofNullable(workflowId).ifPresent(str -> labels.put("workflow-id", str));
			Optional.ofNullable(activityId).ifPresent(str -> labels.put("activity-id", str));
			Optional.ofNullable(taskId).ifPresent(str -> labels.put("task-id", str));
			return labels;
		}
}
