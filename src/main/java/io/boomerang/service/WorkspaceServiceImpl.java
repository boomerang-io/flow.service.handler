package io.boomerang.service;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.boomerang.error.BoomerangError;
import io.boomerang.error.BoomerangException;
import io.boomerang.kube.exception.KubeRuntimeException;
import io.boomerang.kube.service.KubeServiceImpl;
import io.boomerang.model.WorkspaceRequest;
import io.boomerang.model.Response;
import io.boomerang.model.ref.WorkflowWorkspaceSpec;
import io.fabric8.kubernetes.client.KubernetesClientException;

@Service
public class WorkspaceServiceImpl implements WorkspaceService {

  private static final Logger LOGGER = LogManager.getLogger(WorkspaceServiceImpl.class);

  @Value("${kube.workspace.storage.size}")
  protected String storageSize;

  @Value("${kube.workspace.storage.class}")
  protected String storageClassName;

  @Value("${kube.workspace.storage.accessMode}")
  protected String storageAccessMode;

  @Value("${kube.timeout.waitUntil}")
  protected long waitUntilTimeout;

  // @Autowired
  // private KubeServiceImpl kubeService;

  @Autowired
  private KubeServiceImpl kubeService;

  /*
   * Creates the 'workflowRun' and 'workflow' type Workspaces.
   * 
   * TODO: check the types better and error handling
   */
  @Override
  public Response create(WorkspaceRequest workspace) {
    Response response =
        new Response("0", "Workspace (" + workspace.getName() + ") has been created successfully.");
    LOGGER.info("CreateWorkspace Request: " + workspace.toString());
    if (workspace != null
        && ("workflow".equals(workspace.getType()) || "workfowRun".equals(workspace.getType()))) {
      ObjectMapper mapper = new ObjectMapper();
      try {
        // Based on the Workspace Type we set the workspaceRef to be the WorkflowRef or the
        // WorkflowRunRef
        String workspaceRef = getWorkspaceRef(workspace.getType(), workspace.getWorkflowRef(), workspace.getWorkflowRunRef());
        boolean pvcExists =
            kubeService.checkWorkspacePVCExists(workspaceRef, workspace.getType(), false);
        if (!pvcExists && workspace.getSpec() != null) {
          WorkflowWorkspaceSpec spec = mapper.convertValue(workspace.getSpec(), WorkflowWorkspaceSpec.class);
          String size = spec.getSize() == null || spec.getSize().isEmpty() ? storageSize
              : spec.getSize();
          String className =
              spec.getClassName() == null || spec.getClassName().isEmpty() ? storageClassName
                  : spec.getClassName();
          String accessMode =
              spec.getAccessMode() == null || spec.getAccessMode().isEmpty() ? storageAccessMode
                  : spec.getAccessMode();
          kubeService.createWorkspacePVC(workspace.getWorkflowRef(), workspaceRef,
              workspace.getType(), workspace.getLabels(), size, className, accessMode,
              waitUntilTimeout);
        } else if (pvcExists) {
          LOGGER.debug("Workspace (" + workspace.getName() + ") already exists.");
        }
      } catch (KubeRuntimeException | KubernetesClientException | InterruptedException e) {
        LOGGER.error(e.getMessage());
        throw new BoomerangException(e, 1, e.toString(), HttpStatus.INTERNAL_SERVER_ERROR);
      } catch (IllegalArgumentException e) {
        if (e.getMessage().contains("condition not found")) {
          throw new BoomerangException(e, BoomerangError.PVC_CREATE_CONDITION_NOT_MET,
              "" + waitUntilTimeout);
        } else {
          throw new BoomerangException(e, 1, e.toString(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
      }
    } else {
      throw new BoomerangException(500, "Unable to create Workspace",
          HttpStatus.INTERNAL_SERVER_ERROR);
      // TODO: make error more specific
    }
    return response;
  }

  @Override
  public String getWorkspaceRef(String workspaceType, String workflowRef, String workflowRunRef) {
    return "workflow".equals(workspaceType) ? workflowRef
        : workflowRunRef;
  }

  /*
   * Deletes the 'workflowRun' and 'workflow' type Workspaces.
   * 
   * TODO: check the types better and error handling
   */
  @Override
  public Response delete(WorkspaceRequest workspace) {
    Response response =
        new Response("0", "Workspace (" + workspace.getName() + ") has been successfully deleted.");
    try {
      // Based on the Workspace Type we set the workspaceRef to be the WorkflowRef or the
      // WorkflowRunRef
      String workspaceRef = "workflow".equals(workspace.getType()) ? workspace.getWorkflowRef()
          : workspace.getWorkflowRunRef();
      kubeService.deleteWorkspacePVC(workspaceRef, workspace.getType());
    } catch (KubeRuntimeException e) {
      throw new BoomerangException(e, 1, e.toString(), HttpStatus.INTERNAL_SERVER_ERROR);
    }
    LOGGER.info("deleteWorkspace() - " + response.getMessage());
    return response;
  }
}
