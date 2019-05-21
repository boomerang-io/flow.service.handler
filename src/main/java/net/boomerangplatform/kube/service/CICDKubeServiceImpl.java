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
@Profile("cicd")
public class CICDKubeServiceImpl extends AbstractKubeServiceImpl {
	
	@Value("${kube.image}")
	private String kubeImage;
	
	final static String ORG = "bmrg";
	
	final static String PREFIX = ORG + "-cicd";
	
	final static String PREFIX_CFGMAP = PREFIX + "-cfg";
	
	final static String PREFIX_PVC = PREFIX + "-pvc";
	
	final static String PREFIX_VOL = PREFIX + "-vol";
	
	final static String PREFIX_VOL_DATA = PREFIX_VOL + "-data";
	
	final static String PREFIX_VOL_PROPS = PREFIX_VOL + "-props";
	
	@Override
	protected V1Job createJobBody(String componentName, String componentId, String activityId, String taskName, String taskId, List<String> arguments, Map<String, String> taskInputProperties) {

		// Set Variables
		final String volMountPath = "/data";
		final String cfgMapMountPath = "/props";

		// Initialize Job Body
		V1Job body = new V1Job(); // V1Job |
		
		// Create Metadata
		V1ObjectMeta jobMetadata = new V1ObjectMeta();
		jobMetadata.annotations(createAnnotations(componentName, componentId, activityId, taskId));
		jobMetadata.labels(createLabels(componentName, componentId, activityId, taskId));
		jobMetadata.generateName(PREFIX + "-");
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
		if (proxyEnabled) {
			envVars.addAll(createProxyEnvVars());
		}
		container.env(envVars);
		container.args(arguments);
		if (!getPVCName(componentId, activityId).isEmpty()) {
			V1VolumeMount volMount = new V1VolumeMount();
			volMount.name(PREFIX_VOL_DATA);
			volMount.mountPath(volMountPath);
			container.addVolumeMountsItem(volMount);
			V1Volume workerVolume = new V1Volume();
			workerVolume.name(PREFIX_VOL_DATA);
			V1PersistentVolumeClaimVolumeSource workerVolumePVCSource = new V1PersistentVolumeClaimVolumeSource();
			workerVolume.persistentVolumeClaim(workerVolumePVCSource.claimName(getPVCName(componentId, activityId)));
			podSpec.addVolumesItem(workerVolume);
		}
		//Container ConfigMap Mount
		V1VolumeMount volMountConfigMap = new V1VolumeMount();
		volMountConfigMap.name(PREFIX_VOL_PROPS);
		volMountConfigMap.mountPath(cfgMapMountPath);
		container.addVolumeMountsItem(volMountConfigMap);
		
		//Creation of Projected Volume for multiple ConfigMaps
		V1Volume volumeProps = new V1Volume();
		volumeProps.name(PREFIX_VOL_PROPS);
		V1ProjectedVolumeSource projectedVolPropsSource = new V1ProjectedVolumeSource();
		List<V1VolumeProjection> projectPropsVolumeList = new ArrayList<V1VolumeProjection>();
		
		//Add Worfklow Configmap Projected Volume
		V1ConfigMap wfConfigMap = getConfigMap(componentId, activityId, null);
		if (wfConfigMap != null && !getConfigMapName(wfConfigMap).isEmpty()) {
			V1ConfigMapProjection projectedConfigMapWorkflow = new V1ConfigMapProjection();
			projectedConfigMapWorkflow.name(getConfigMapName(wfConfigMap));
			V1VolumeProjection configMapVolSourceWorkflow = new V1VolumeProjection();
			configMapVolSourceWorkflow.configMap(projectedConfigMapWorkflow);
			projectPropsVolumeList.add(configMapVolSourceWorkflow);	
		}
		//Add Task Configmap Projected Volume
		V1ConfigMap taskConfigMap = getConfigMap(componentId, activityId, taskId);
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
		podMetadata.annotations(createAnnotations(componentName, componentId, activityId, taskId));
		podMetadata.labels(createLabels(componentName, componentId, activityId, taskId));
		templateSpec.metadata(podMetadata);
		
		jobSpec.backoffLimit(kubeWorkerJobBackOffLimit);
		jobSpec.template(templateSpec);
		body.spec(jobSpec);
		
		return body;
	}
	
	protected V1ConfigMap createTaskConfigMapBody(
		      String componentName,
		      String componentId,
		      String activityId,
		      String taskName,
		      String taskId,
		      Map<String, String> inputProps) {
		    V1ConfigMap body = new V1ConfigMap();
		    
		    // Create Metadata
		    V1ObjectMeta metadata = new V1ObjectMeta();
		    metadata.annotations(createAnnotations(componentName, componentId, activityId, taskId));
		    metadata.labels(createLabels(componentName, componentId, activityId, taskId));
		    metadata.generateName(PREFIX_CFGMAP);
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
			      String componentName,
			      String componentId,
			      String activityId,
			      Map<String, String> inputProps) {
			    V1ConfigMap body = new V1ConfigMap();
			    
			    // Create Metadata
			    V1ObjectMeta metadata = new V1ObjectMeta();
			    metadata.annotations(createAnnotations(componentName, componentId, activityId, null));
			    metadata.labels(createLabels(componentName, componentId, activityId, null));
			    metadata.generateName(PREFIX_CFGMAP);
			    body.metadata(metadata);
			    
			    //Create Data
			    Map<String, String> inputsWithFixedKeys = new HashMap<String, String>();
			    Map<String, String> sysProps = new HashMap<String, String>();
			    sysProps.put("activity.id", activityId);
			    sysProps.put("workflow.name", componentName);
			    sysProps.put("workflow.id", componentId);
			    sysProps.put("worker.debug", kubeWorkerDebug.toString());
			    sysProps.put("controller.service.url", bmrgControllerServiceURL);
			    inputsWithFixedKeys.put("workflow.input.properties", createConfigMapProp(inputProps));
			    inputsWithFixedKeys.put("workflow.system.properties", createConfigMapProp(sysProps));
			    body.data(inputsWithFixedKeys);
			    return body;
			  }

  protected String getLabelSelector(String componentId, String activityId, String taskId) {
    String labelSelector =
        "org=" + ORG + ",app=" + PREFIX + ",component-id="
            + componentId
            + ",activity-id="
            + activityId;
    return Optional.ofNullable(taskId).isPresent() ? labelSelector.concat(",task-id=" + taskId) : labelSelector;
  }
  
	protected Map<String, String> createAnnotations(String componentName, String componentId, String activityId, String taskId) {
		Map<String, String> annotations = new HashMap<String, String>();
		annotations.put("boomerangplatform.net/org", ORG);
		annotations.put("boomerangplatform.net/app", PREFIX);
		annotations.put("boomerangplatform.net/component-name", componentName);
		annotations.put("boomerangplatform.net/component-id", componentId);
		annotations.put("boomerangplatform.net/activity-id", activityId);
		Optional.ofNullable(taskId).ifPresent(str -> annotations.put("boomerangplatform.net/task-id", str));
		
		return annotations;
	}
	
	protected Map<String, String> createLabels(String componentName, String componentId, String activityId, String taskId) {
		Map<String, String> labels = new HashMap<String, String>();
		labels.put("org", ORG);
		labels.put("app", PREFIX);
		Optional.ofNullable(componentName).ifPresent(str -> labels.put("component-name", str.replace(" ", "")));
		Optional.ofNullable(componentId).ifPresent(str -> labels.put("component-id", str));
		Optional.ofNullable(activityId).ifPresent(str -> labels.put("activity-id", str));
		Optional.ofNullable(taskId).ifPresent(str -> labels.put("task-id", str));
		return labels;
	}
}
