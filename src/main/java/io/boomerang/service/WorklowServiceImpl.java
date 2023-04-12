package io.boomerang.service;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import io.boomerang.error.BoomerangException;
import io.boomerang.kube.exception.KubeRuntimeException;
import io.boomerang.kube.service.KubeService;
import io.boomerang.model.WorkflowRequest;
import io.boomerang.model.WorkspaceRequest;
import io.boomerang.model.Response;
import io.fabric8.kubernetes.client.KubernetesClientException;

@Service
public class WorklowServiceImpl implements WorkflowService {

  private static final Logger LOGGER = LogManager.getLogger(WorklowServiceImpl.class);

  @Autowired
  private KubeService kubeService;

  @Autowired
  private WorkspaceService workspaceService;

  /*
   * Creates the resources need for a Workflow. At this point in time the resources consist of
   * Workspace PVC's of type workflow or workflowRun. It will check if they are created prior.
   * 
   * It will move the workflow from Status: Ready, Phase: Pending to Status: Running, Phase: Running
   * and return the information to the Engine.
   */
  @Override
  public Response execute(WorkflowRequest workflow) {
    Response response = new Response("0",
        "WorkflowRun (" + workflow.getWorkflowRunRef() + ") has been created successfully.");
    LOGGER.info(workflow.toString());
    if (workflow.getWorkspaces() != null && !workflow.getWorkspaces().isEmpty()) {
      workflow.getWorkspaces().stream()
          .filter(ws -> "workflow".equals(ws.getType()) || "workfowRun".equals(ws.getType()))
          .forEach(ws -> {
            try {
              // Based on the Workspace Type we set the workspaceRef to be the WorkflowRef or the
              // WorkflowRunRef
              String workspaceRef = workspaceService.getWorkspaceRef(ws.getType(), workflow.getWorkflowRef(), workflow.getWorkflowRunRef());
              boolean pvcExists =
                  kubeService.checkWorkspacePVCExists(workspaceRef, ws.getType(), false);
              if (!pvcExists && ws.getSpec() != null) {
                WorkspaceRequest request = new WorkspaceRequest();
                request.setName(ws.getName());
                request.setLabels(workflow.getLabels());
                request.setType(ws.getType());
                request.setOptional(ws.isOptional());
                request.setSpec(ws.getSpec());
                request.setWorkflowRef(workflow.getWorkflowRef());
                request.setWorkflowRunRef(workflow.getWorkflowRunRef());
                workspaceService.create(request);
              } else if (pvcExists) {
                LOGGER.debug("Workspace (" + ws.getName() + ") PVC already existed.");
              }
            } catch (KubeRuntimeException | KubernetesClientException e) {
              LOGGER.error(e.getMessage());
              throw new BoomerangException(e, 1, e.toString(), HttpStatus.INTERNAL_SERVER_ERROR);
            } 
          });
    } else {
      response = new Response("0",
          "WorkflowRun (" + workflow.getWorkflowRunRef() + ") created without workspaces.");
    }
    return response;
  }

  /*
   * Ends the workflow, removes the resources used by the WorkflowRun and moves the phase from
   * completed to finalized, respecting the status based on the Workflow.
   * 
   * At this point in time the resources are Workspaces and this only removes the 'workflowRun'
   * Workspaces as 'workflow' Workspaces persist across executions.
   */
  @Override
  public Response terminate(WorkflowRequest workflow) {
    Response response = new Response("0",
        "WorkflowRun (" + workflow.getWorkflowRunRef() + ") has been terminated successfully.");
    if (workflow.getWorkspaces() != null && !workflow.getWorkspaces().isEmpty()) {
      workflow.getWorkspaces().stream().filter(ws -> "workfowRun".equals(ws.getType()))
          .forEach(ws -> {
            WorkspaceRequest request = new WorkspaceRequest();
            request.setType(ws.getType());
            request.setWorkflowRef(workflow.getWorkflowRef());
            request.setWorkflowRunRef(workflow.getWorkflowRunRef());
            workspaceService.delete(request);
          });
    } else {
      response = new Response("0", "WorkflowRun (" + workflow.getWorkflowRunRef()
          + ") terminated without removing Workspaces.");
    }
    return response;
  }
}
