package net.boomerangplatform.kube.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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
	
	final static String PREFIX = "bmrg-flow-";
	
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
		jobMetadata.generateName(PREFIX);
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
			volMount.name(PREFIX_VOL + "data");
			volMount.mountPath(volMountPath);
			container.addVolumeMountsItem(volMount);
			V1Volume workerVolume = new V1Volume();
			workerVolume.name(PREFIX_VOL + "data");
			V1PersistentVolumeClaimVolumeSource workerVolumePVCSource = new V1PersistentVolumeClaimVolumeSource();
			workerVolume.persistentVolumeClaim(workerVolumePVCSource.claimName(getPVCName(workflowId, activityId)));
			podSpec.addVolumesItem(workerVolume);
		}
		V1ConfigMap wfConfigMap = getConfigMap(workflowId, activityId, taskId);
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
			projectedConfigMapWorkflow.name(PREFIX_CFGMAP + activityId);
			V1VolumeProjection configMapVolSourceWorkflow = new V1VolumeProjection();
			configMapVolSourceWorkflow.configMap(projectedConfigMapWorkflow);
			projectPropsVolumeList.add(configMapVolSourceWorkflow);
			
			V1ConfigMapProjection projectedConfigMapTask = new V1ConfigMapProjection();
			projectedConfigMapTask.name(PREFIX_CFGMAP + activityId + "-" + taskId);
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
		podMetadata.annotations(createAnnotations(workflowName, workflowId, activityId, taskId));
		podMetadata.labels(createLabels(workflowName, workflowId, activityId, taskId));
		templateSpec.metadata(podMetadata);
		
		jobSpec.backoffLimit(kubeWorkerJobBackOffLimit);
		jobSpec.template(templateSpec);
		body.spec(jobSpec);
		
		return body;
	}
	
	  protected String getLabelSelector(String workflowId, String workflowActivityId) {
		    String labelSelector = "org=bmrg,app="+PREFIX+",workflow-id="+workflowId+",workflow-activity-id="+workflowActivityId;
		    return labelSelector;
		  }
	
	  protected String getLabelSelector(String workflowId, String workflowActivityId, String taskId) {
		    String labelSelector = "org=bmrg,app="+PREFIX+",workflow-id="+workflowId+",workflow-activity-id="+workflowActivityId+",task-id="+taskId;
		    return labelSelector;
		  }
}
