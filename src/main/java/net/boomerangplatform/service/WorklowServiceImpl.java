package net.boomerangplatform.service;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import io.kubernetes.client.ApiException;
import net.boomerangplatform.error.BoomerangException;
import net.boomerangplatform.kube.exception.KubeRuntimeException;
import net.boomerangplatform.kube.service.KubeServiceImpl;
import net.boomerangplatform.model.Response;
import net.boomerangplatform.model.Workflow;

@Service
public class WorklowServiceImpl implements WorkflowService {

  private static final Logger LOGGER = LogManager.getLogger(WorklowServiceImpl.class);
    @Autowired
    private KubeServiceImpl kubeService;

    @Override
    public Response createWorkflow(Workflow workflow) {
      Response response = new Response("0", "Workflow Activity (" + workflow.getWorkflowActivityId()
          + ") has been created successfully.");
      try {
          LOGGER.info(workflow.toString());
        if (workflow.getWorkflowStorage().getEnable()) {
          kubeService.createWorkflowPVC(workflow.getWorkflowName(), workflow.getWorkflowId(),
              workflow.getWorkflowActivityId(), workflow.getWorkflowStorage().getSize());
          kubeService.watchWorkflowPVC(workflow.getWorkflowId(), workflow.getWorkflowActivityId()).getPhase();
        }
        kubeService.createWorkflowConfigMap(workflow.getWorkflowName(), workflow.getWorkflowId(),
            workflow.getWorkflowActivityId(), workflow.getParameters());
        kubeService.watchConfigMap(workflow.getWorkflowId(), workflow.getWorkflowActivityId(), null);
      } catch (ApiException | KubeRuntimeException e) {
            throw new BoomerangException(e, 1, e.toString(), HttpStatus.INTERNAL_SERVER_ERROR);
      }
      return response;
    }

    @Override
    public Response terminateWorkflow(Workflow workflow) {
      Response response = new Response("0", "Workflow Activity (" + workflow.getWorkflowActivityId()
          + ") has been terminated successfully.");
      try {
        kubeService.deleteWorkflowPVC(workflow.getWorkflowId(), workflow.getWorkflowActivityId());
        kubeService.deleteConfigMap(workflow.getWorkflowId(), workflow.getWorkflowActivityId(), null);
      } catch (KubeRuntimeException e) {
            throw new BoomerangException(e, 1, e.toString(), HttpStatus.INTERNAL_SERVER_ERROR);
      }
      return response;
    }
}
