// package io.boomerang.service;

// import static org.junit.Assert.assertEquals;
// import static org.junit.Assert.assertNotNull;
// import static org.junit.Assert.assertNull;
// import static org.junit.Assert.assertTrue;
// import static org.mockito.Mockito.mock;
// import java.util.ArrayList;
// import java.util.HashMap;
// import java.util.Map;
// import javax.servlet.http.HttpServletResponse;

// import org.junit.Ignore;
// import org.junit.Test;
// import org.junit.runner.RunWith;
// import org.mockito.Mockito;
// import org.springframework.beans.factory.annotation.Autowired;
// import org.springframework.boot.test.context.SpringBootTest;
// import org.springframework.boot.test.mock.mockito.MockBean;
// import org.springframework.mock.web.MockHttpServletResponse;
// import org.springframework.test.context.ActiveProfiles;
// import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
// import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;
// import io.boomerang.kube.exception.KubeRuntimeException;
// import io.boomerang.kube.service.CICDKubeServiceImpl;
// import io.boomerang.model.Response;
// import io.boomerang.model.Storage;
// import io.boomerang.model.Task;
// import io.boomerang.model.TaskResponse;
// import io.boomerang.model.Workflow;
// import io.kubernetes.client.ApiException;
// import io.kubernetes.client.models.V1ConfigMap;
// import io.kubernetes.client.models.V1Job;
// import io.kubernetes.client.models.V1PersistentVolumeClaim;
// import io.kubernetes.client.models.V1PersistentVolumeClaimStatus;
// import io.kubernetes.client.models.V1Status;

// @RunWith(SpringJUnit4ClassRunner.class)
// @SpringBootTest
// @ActiveProfiles("cicd")
// public class CICDControllerServiceTest {

//   @Autowired
//   private CICDControllerServiceImpl cicdControllerService;

//   @MockBean
//   private CICDKubeServiceImpl kubeService;

//   @Test
//   public void testCreateWorkflow() throws ApiException {
//     Workflow workflow = getDefaultWorkflow();
//     Storage storage = new Storage();
//     storage.setEnable(true);
//     workflow.setWorkflowStorage(storage);

//     V1PersistentVolumeClaim claim = new V1PersistentVolumeClaim();
//     V1PersistentVolumeClaimStatus status = new V1PersistentVolumeClaimStatus();
//     status.setPhase("phase1");
//     Mockito.when(kubeService.createPVC("workflowName", "workflowId", "workflowActivityId", null))
//         .thenReturn(claim);
//     Mockito.when(kubeService.watchPVC("workflowId", "workflowActivityId")).thenReturn(status);

//     Response response = cicdControllerService.createWorkflow(workflow);
//     assertEquals("0", response.getCode());
//     assertTrue(response.getMessage().startsWith("Component Activity (workflowActivityId"));

//     Mockito.verify(kubeService).createPVC("workflowName", "workflowId", "workflowActivityId", null);
//     Mockito.verify(kubeService).watchPVC("workflowId", "workflowActivityId");
//   }

//   @Test
//   public void testCreateWorkflowWithStorageDisabled() throws ApiException {
//     Workflow workflow = getDefaultWorkflow();
//     Storage storage = new Storage();
//     storage.setEnable(false);
//     workflow.setWorkflowStorage(storage);

//     Response response = cicdControllerService.createWorkflow(workflow);
//     assertEquals("0", response.getCode());
//     assertTrue(response.getMessage().startsWith("Component Activity (workflowActivityId"));

//     Mockito.verifyNoMoreInteractions(kubeService);
//   }

//   @Test
//   public void testCreateWorkflowWithException() throws ApiException {
//     Workflow workflow = getDefaultWorkflow();
//     Storage storage = new Storage();
//     storage.setEnable(true);
//     workflow.setWorkflowStorage(storage);

//     Mockito.when(kubeService.createPVC("workflowName", "workflowId", "workflowActivityId", null))
//         .thenThrow(KubeRuntimeException.class);

//     Response response = cicdControllerService.createWorkflow(workflow);
//     assertEquals("1", response.getCode());
//     assertTrue(response.getMessage()
//         .startsWith("net.boomerangplatform.kube.exception.KubeRuntimeException"));

//     Mockito.verify(kubeService).createPVC("workflowName", "workflowId", "workflowActivityId", null);
//   }

//   @Test
//   public void testTerminateWorkflow() {
//     Workflow workflow = getDefaultWorkflow();
//     Mockito.when(kubeService.deletePVC(workflow.getWorkflowId(), null)).thenReturn(new V1Status());

//     Response response = cicdControllerService.terminateWorkflow(workflow);
//     assertEquals("0", response.getCode());
//     assertTrue(response.getMessage().startsWith("Component Storage (workflowId"));

//     Mockito.verify(kubeService).deletePVC(workflow.getWorkflowId(), null);
//   }

//   @Test
//   public void testTerminateWorkflowWithException() throws ApiException {
//     Workflow workflow = getDefaultWorkflow();
//     Mockito.when(kubeService.deletePVC(workflow.getWorkflowId(), null))
//         .thenThrow(KubeRuntimeException.class);

//     Response response = cicdControllerService.terminateWorkflow(workflow);
//     assertEquals("1", response.getCode());
//     assertTrue(response.getMessage()
//         .startsWith("net.boomerangplatform.kube.exception.KubeRuntimeException"));

//     Mockito.verify(kubeService).deletePVC(workflow.getWorkflowId(), null);
//   }

//   @Test
//   public void testExecuteTask() {
//     Task task = getDefaultTask();

//     Mockito.when(kubeService.checkPVCExists(task.getWorkflowId(), null, null, true))
//         .thenReturn(false);
//     Mockito.when(kubeService.createTaskConfigMap(task.getWorkflowName(), task.getWorkflowId(),
//         task.getWorkflowActivityId(), task.getTaskName(), task.getTaskId(),
//         task.getProperties())).thenReturn(new V1ConfigMap());
//     Mockito.when(kubeService.watchConfigMap(task.getWorkflowId(), task.getWorkflowActivityId(),
//         task.getTaskId())).thenReturn(new V1ConfigMap());
//     Mockito.when(kubeService.createJob(task.getWorkflowName(), task.getWorkflowId(),
//         task.getWorkflowActivityId(), task.getTaskActivityId(), task.getTaskName(), task.getTaskId(), task.getArguments(),
//         task.getProperties())).thenReturn(new V1Job());
//     Mockito.when(
//         kubeService.watchJob(task.getWorkflowId(), task.getWorkflowActivityId(), task.getTaskId()))
//         .thenReturn(new V1Job());

//     Mockito.when(kubeService.deleteConfigMap(task.getWorkflowId(), task.getWorkflowActivityId(),
//         task.getTaskId())).thenReturn(new V1Status());

//     TaskResponse response = cicdControllerService.executeTask(task);
//     assertEquals("0", response.getCode());
//     assertTrue(response.getMessage().startsWith("Task (taskId) has been executed successfully."));

//     Mockito.verify(kubeService).checkPVCExists(task.getWorkflowId(), null, null, true);
//     Mockito.verify(kubeService).createTaskConfigMap(task.getWorkflowName(), task.getWorkflowId(),
//         task.getWorkflowActivityId(), task.getTaskName(), task.getTaskId(),
//         task.getProperties());
//     Mockito.verify(kubeService).watchConfigMap(task.getWorkflowId(), task.getWorkflowActivityId(),
//         task.getTaskId());
//     Mockito.verify(kubeService).createJob(task.getWorkflowName(), task.getWorkflowId(),
//     	task.getWorkflowActivityId(), task.getTaskActivityId(), task.getTaskName(), task.getTaskId(), task.getArguments(),
//         task.getProperties());
//     Mockito.verify(kubeService).watchJob(task.getWorkflowId(), task.getWorkflowActivityId(),
//         task.getTaskId());

//     Mockito.verify(kubeService).deleteConfigMap(task.getWorkflowId(), task.getWorkflowActivityId(),
//         task.getTaskId());
//   }

//   @Test
//   @Ignore 
//   public void testExecuteTaskWithCache() throws ApiException {
//     Task task = getDefaultTask();
//     task.setProperty("component/cache.enabled", "true");

//     Mockito.when(kubeService.checkPVCExists(task.getWorkflowId(), null, null, true))
//         .thenReturn(false);
//     Mockito.when(kubeService.createPVC(task.getWorkflowName(), task.getWorkflowId(),
//         task.getWorkflowActivityId(), null)).thenReturn(new V1PersistentVolumeClaim());
//     Mockito.when(kubeService.watchPVC(task.getWorkflowId(), task.getWorkflowActivityId()))
//         .thenReturn(new V1PersistentVolumeClaimStatus());
//     Mockito.when(kubeService.createTaskConfigMap(task.getWorkflowName(), task.getWorkflowId(),
//         task.getWorkflowActivityId(), task.getTaskName(), task.getTaskId(),
//         task.getProperties())).thenReturn(new V1ConfigMap());
//     Mockito.when(kubeService.watchConfigMap(task.getWorkflowId(), task.getWorkflowActivityId(),
//         task.getTaskId())).thenReturn(new V1ConfigMap());
//     Mockito.when(kubeService.createJob(task.getWorkflowName(), task.getWorkflowId(),
//         task.getWorkflowActivityId(), task.getTaskActivityId(), task.getTaskName(), task.getTaskId(), task.getArguments(),
        
//         task.getProperties())).thenReturn(new V1Job());
//     Mockito.when(
//         kubeService.watchJob(task.getWorkflowId(), task.getWorkflowActivityId(), task.getTaskId()))
//         .thenReturn(new V1Job());

//     Mockito.when(kubeService.deleteConfigMap(task.getWorkflowId(), task.getWorkflowActivityId(),
//         task.getTaskId())).thenReturn(new V1Status());

//     TaskResponse response = cicdControllerService.executeTask(task);
//     assertEquals("0", response.getCode());
//     assertTrue(response.getMessage().startsWith("Task (taskId) has been executed successfully."));

//     Mockito.verify(kubeService).checkPVCExists(task.getWorkflowId(), null, null, true);
//     Mockito.verify(kubeService).createPVC(task.getWorkflowName(), task.getWorkflowId(),
//         task.getWorkflowActivityId(), null);
//     Mockito.verify(kubeService).watchPVC(task.getWorkflowId(), task.getWorkflowActivityId());

//     Mockito.verify(kubeService).createTaskConfigMap(task.getWorkflowName(), task.getWorkflowId(),
//         task.getWorkflowActivityId(), task.getTaskName(), task.getTaskId(),
//         task.getProperties());
//     Mockito.verify(kubeService).watchConfigMap(task.getWorkflowId(), task.getWorkflowActivityId(),
//         task.getTaskId());
//     Mockito.verify(kubeService).createJob(task.getWorkflowName(), task.getWorkflowId(),
//         task.getWorkflowActivityId(), task.getTaskActivityId(), task.getTaskName(), task.getTaskId(), task.getArguments(),
//         task.getProperties());
//     Mockito.verify(kubeService).watchJob(task.getWorkflowId(), task.getWorkflowActivityId(),
//         task.getTaskId());

//     Mockito.verify(kubeService).deleteConfigMap(task.getWorkflowId(), task.getWorkflowActivityId(),
//         task.getTaskId());
//   }

  
  
//   @Test
//   public void testExecuteTaskWithException() {
//     Task task = getDefaultTask();
//     task.setArguments(new ArrayList<>());
//     task.setArgument("argument");

//     Mockito.when(kubeService.checkPVCExists(task.getWorkflowId(), null, null, true))
//         .thenReturn(true);
//     Mockito.when(kubeService.deletePVC(task.getWorkflowId(), null)).thenReturn(new V1Status());
//     Mockito.when(kubeService.createTaskConfigMap(task.getWorkflowName(), task.getWorkflowId(),
//         task.getWorkflowActivityId(), task.getTaskName(), task.getTaskId(),
//         task.getProperties())).thenThrow(KubeRuntimeException.class);
//     Mockito.when(kubeService.deleteConfigMap(task.getWorkflowId(), task.getWorkflowActivityId(),
//         task.getTaskId())).thenReturn(new V1Status());

//     TaskResponse response = cicdControllerService.executeTask(task);
//     assertEquals("1", response.getCode());
//     assertTrue(response.getMessage()
//         .startsWith("net.boomerangplatform.kube.exception.KubeRuntimeException"));

//     Mockito.verify(kubeService).checkPVCExists(task.getWorkflowId(), null, null, true);
//     Mockito.verify(kubeService).deletePVC(task.getWorkflowId(), null);
//     Mockito.verify(kubeService).createTaskConfigMap(task.getWorkflowName(), task.getWorkflowId(),
//         task.getWorkflowActivityId(), task.getTaskName(), task.getTaskId(),
//         task.getProperties());
//     Mockito.verify(kubeService).deleteConfigMap(task.getWorkflowId(), task.getWorkflowActivityId(),
//         task.getTaskId());
//   }

//   @Test
//   public void testSetJobOutputProperty() {
//     String workflowId = "workflowId";
//     String workflowActivityId = "workflowActivityId";
//     String taskId = "taskId";
//     String taskName = "taskName";
//     Map<String, String> properties = new HashMap<>();
//     properties.put("key", "value");
//     Mockito.doNothing().when(kubeService).patchTaskConfigMap(workflowId, workflowActivityId, taskId,
//         taskName, properties);

//     Response response = cicdControllerService.setTaskResultParameter(workflowId, workflowActivityId,
//         taskId, taskName, "key", "value");
//     assertEquals("0", response.getCode());
//     assertTrue(response.getMessage().startsWith(
//         "Property has been set against workflow (workflowActivityId) and task (taskId)"));

//     Mockito.verify(kubeService).patchTaskConfigMap(workflowId, workflowActivityId, taskId, taskName,
//         properties);
//   }

//   @Test
//   public void testSetJobOutputPropertyWithException() {
//     String workflowId = "workflowId";
//     String workflowActivityId = "workflowActivityId";
//     String taskId = "taskId";
//     String taskName = "taskName";
//     Map<String, String> properties = new HashMap<>();
//     properties.put("key", "value");
//     Mockito.doThrow(KubeRuntimeException.class).when(kubeService).patchTaskConfigMap(workflowId,
//         workflowActivityId, taskId, taskName, properties);

//     Response response = cicdControllerService.setTaskResultParameter(workflowId, workflowActivityId,
//         taskId, taskName, "key", "value");
//     assertEquals("1", response.getCode());
//     assertTrue(response.getMessage()
//         .startsWith("net.boomerangplatform.kube.exception.KubeRuntimeException"));

//     Mockito.verify(kubeService).patchTaskConfigMap(workflowId, workflowActivityId, taskId, taskName,
//         properties);
//   }

//   @Test
//   public void testSetJobOutputProperties() {
//     String workflowId = "workflowId";
//     String workflowActivityId = "workflowActivityId";
//     String taskId = "taskId";
//     String taskName = "taskName";
//     Map<String, String> properties = new HashMap<>();
//     properties.put("key", "value");
//     Mockito.doNothing().when(kubeService).patchTaskConfigMap(workflowId, workflowActivityId, taskId,
//         taskName, properties);

//     Response response = cicdControllerService.setTaskResultParameters(workflowId, workflowActivityId,
//         taskId, taskName, properties);
//     assertEquals("0", response.getCode());
//     assertTrue(response.getMessage().startsWith(
//         "Properties have been set against workflow (workflowActivityId) and task (taskId)"));

//     Mockito.verify(kubeService).patchTaskConfigMap(workflowId, workflowActivityId, taskId, taskName,
//         properties);
//   }

//   @Test
//   public void testSetJobOutputPropertiesWithException() {
//     String workflowId = "workflowId";
//     String workflowActivityId = "workflowActivityId";
//     String taskId = "taskId";
//     String taskName = "taskName";
//     Map<String, String> properties = new HashMap<>();
//     properties.put("key", "value");
//     Mockito.doThrow(KubeRuntimeException.class).when(kubeService).patchTaskConfigMap(workflowId,
//         workflowActivityId, taskId, taskName, properties);

//     Response response = cicdControllerService.setTaskResultParameters(workflowId, workflowActivityId,
//         taskId, taskName, properties);
//     assertEquals("1", response.getCode());
//     assertTrue(response.getMessage()
//         .startsWith("net.boomerangplatform.kube.exception.KubeRuntimeException"));

//     Mockito.verify(kubeService).patchTaskConfigMap(workflowId, workflowActivityId, taskId, taskName,
//         properties);
//   }

//   @Test
//   public void testGetLogForTask() {
//     String workflowId = "workflowId";
//     String workflowActivityId = "workflowActivityId";
//     String taskId = "taskId";

//     Mockito.when(kubeService.getPodLog(workflowId, workflowActivityId, taskId,taskId))
//         .thenReturn("Success");

//     Response response = cicdControllerService.getLogForTask(workflowId, workflowActivityId, taskId,taskId);
//     assertEquals("0", response.getCode());
//     assertEquals("Success", response.getMessage());

//     Mockito.verify(kubeService).getPodLog(workflowId, workflowActivityId, taskId,taskId);
//   }

//   @Test
//   public void testGetLogForTaskWithException() {
//     String workflowId = "workflowId";
//     String workflowActivityId = "workflowActivityId";
//     String taskId = "taskId";

//     Mockito.when(kubeService.getPodLog(workflowId, workflowActivityId, taskId,taskId))
//         .thenThrow(KubeRuntimeException.class);

//     Response response = cicdControllerService.getLogForTask(workflowId, workflowActivityId, taskId,taskId);
//     assertEquals("1", response.getCode());
//     assertTrue(response.getMessage()
//         .startsWith("net.boomerangplatform.kube.exception.KubeRuntimeException"));

//     Mockito.verify(kubeService).getPodLog(workflowId, workflowActivityId, taskId,taskId);
//   }

//   @Test
//   public void testStreamLogForTask() {
//     String workflowId = "workflowId";
//     String workflowActivityId = "workflowActivityId";
//     String taskId = "taskId";
//     HttpServletResponse response = new MockHttpServletResponse();

//     Mockito.when(kubeService.streamPodLog(response, workflowId, workflowActivityId, taskId,taskId))
//         .thenReturn(mock(StreamingResponseBody.class));

//     StreamingResponseBody streamingResponseBody =
//         cicdControllerService.streamLogForTask(response, workflowId, workflowActivityId, taskId,taskId);
//     assertNotNull(streamingResponseBody);

//     Mockito.verify(kubeService).streamPodLog(response, workflowId, workflowActivityId, taskId,taskId);
//   }

//   @Test
//   public void testStreamLogForTaskWithException() {
//     String workflowId = "workflowId";
//     String workflowActivityId = "workflowActivityId";
//     String taskId = "taskId";
//     HttpServletResponse response = new MockHttpServletResponse();

//     Mockito.when(kubeService.streamPodLog(response, workflowId, workflowActivityId, taskId,taskId))
//         .thenThrow(KubeRuntimeException.class);

//     StreamingResponseBody streamingResponseBody =
//         cicdControllerService.streamLogForTask(response, workflowId, workflowActivityId, taskId,taskId);
//     assertNull(streamingResponseBody);

//     Mockito.verify(kubeService).streamPodLog(response, workflowId, workflowActivityId, taskId,taskId);
//   }

//   private Workflow getDefaultWorkflow() {
//     Workflow workflow = new Workflow();
//     workflow.setWorkflowActivityId("workflowActivityId");
//     workflow.setWorkflowName("workflowName");
//     workflow.setWorkflowId("workflowId");
//     return workflow;
//   }

//   private Task getDefaultTask() {
//     Task task = new Task();
//     task.setWorkflowName("workflowName");
//     task.setWorkflowId("workflowId");
//     task.setWorkflowActivityId("workflowActivityId");
//     task.setTaskId("taskId");
//     task.setTaskName("taskName");
//     task.setProperties(new HashMap<>());
//     task.setProperty("name1", "value1");
//     return task;
//   }
// }
