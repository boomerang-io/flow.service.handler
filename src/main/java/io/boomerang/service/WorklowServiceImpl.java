package io.boomerang.service;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import io.boomerang.error.BoomerangError;
import io.boomerang.error.BoomerangException;
import io.boomerang.kube.exception.KubeRuntimeException;
import io.boomerang.kube.service.KubeServiceImpl;
import io.boomerang.model.Response;
import io.boomerang.model.Workflow;
import io.fabric8.kubernetes.client.KubernetesClientException;

@Service
public class WorklowServiceImpl implements WorkflowService {

  private static final Logger LOGGER = LogManager.getLogger(WorklowServiceImpl.class);
  
  @Value("${kube.workspace.storage.size}")
  protected String storageSize;
  
  @Value("${kube.workspace.storage.class}")
  protected String storageClassName;
  
  @Value("${kube.workspace.storage.accessMode}")
  protected String storageAccessMode;
  
  @Value("${kube.timeout.waitUntil}")
  protected long waitUntilTimeout;
  
  
  
    @Autowired
    private KubeServiceImpl kubeService;

    @Override
    public Response createWorkflow(Workflow workflow) {
      Response response = new Response("0", "Workflow Activity (" + workflow.getWorkflowActivityId()
          + ") has been created successfully.");
      try {
          LOGGER.info(workflow.toString());
        if (workflow.getWorkflowStorage().getEnable()) {
          String size = workflow.getWorkflowStorage().getSize() == null || workflow.getWorkflowStorage().getSize().isEmpty() ? storageSize : workflow.getWorkflowStorage().getSize();
          String className = workflow.getWorkflowStorage().getClassName();
          String accessMode = workflow.getWorkflowStorage().getAccessMode() == null || workflow.getWorkflowStorage().getAccessMode().isEmpty() ? storageAccessMode : workflow.getWorkflowStorage().getAccessMode();
          kubeService.createWorkflowPVC(workflow.getWorkflowName(), workflow.getWorkflowId(),
              workflow.getWorkflowActivityId(), workflow.getLabels(), size, className, accessMode, waitUntilTimeout);
        } else {
          response = new Response("0", "Workflow Activity (" +workflow.getWorkflowActivityId() + ") created without storage.");
        }
      } catch (KubeRuntimeException | KubernetesClientException | InterruptedException e) {
        LOGGER.error(e.getMessage());
        throw new BoomerangException(e, 1, e.toString(), HttpStatus.INTERNAL_SERVER_ERROR);
      } catch (IllegalArgumentException e) {
        if (e.getMessage().contains("condition not found")) {
          throw new BoomerangException(e, BoomerangError.PVC_CREATE_CONDITION_NOT_MET, "" + waitUntilTimeout);
        } else {
          throw new BoomerangException(e, 1, e.toString(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
      }
      return response;
    }

    @Override
    public Response terminateWorkflow(Workflow workflow) {
      Response response = new Response("0", "Workflow Activity (" + workflow.getWorkflowActivityId()
          + ") has been terminated successfully.");
      try {
        kubeService.deleteWorkflowPVC(workflow.getWorkflowId(), workflow.getWorkflowActivityId());
        kubeService.deleteWorkflowConfigMap(workflow.getWorkflowId(), workflow.getWorkflowActivityId());
      } catch (Exception e) {
            throw new BoomerangException(e, 1, e.toString(), HttpStatus.INTERNAL_SERVER_ERROR);
      }
      return response;
    }
}
