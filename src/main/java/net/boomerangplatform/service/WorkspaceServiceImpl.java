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
import net.boomerangplatform.model.Workspace;

@Service
public class WorkspaceServiceImpl implements WorkspaceService {

  private static final Logger LOGGER = LogManager.getLogger(WorkspaceServiceImpl.class);

    @Autowired
    private KubeServiceImpl kubeService;
	
    @Override
    public Response createWorkspace(Workspace workspace) {
      Response response =
          new Response("0", "Workspace (" + workspace.getId() + ") PVC has been created successfully.");
      try {
        LOGGER.info("Workspace: " + workspace.toString());
        boolean cacheExists = kubeService.checkWorkspacePVCExists(workspace.getId(), false);
        if (workspace.getStorage().getEnable() && !cacheExists) {
          kubeService.createWorkspacePVC(workspace.getName(), workspace.getId(), workspace.getStorage().getSize());
          kubeService.watchWorkspacePVC(workspace.getId());
        } else if (cacheExists) {
          response = new Response("0", "Workspace (" + workspace.getId() + ") PVC already existed.");
        }
      } catch (ApiException | KubeRuntimeException e) {
        LOGGER.error(e.getMessage());
        throw new BoomerangException(e, 1, e.toString(), HttpStatus.INTERNAL_SERVER_ERROR);
      }
      LOGGER.info("createWorkspace() - " + response.getMessage());
      return response;
    }

    @Override
    public Response deleteWorkspace(Workspace workspace) {
      Response response =
          new Response("0", "Workspace (" + workspace.getId() + ") has been deleted successfully.");
      try {
        LOGGER.info(workspace.toString());
        kubeService.deleteWorkspacePVC(workspace.getId());
      } catch (KubeRuntimeException e) {
        throw new BoomerangException(e, 1, e.toString(), HttpStatus.INTERNAL_SERVER_ERROR);
      }
      LOGGER.info("createWorkspace() - " + response.getMessage());
      return response;
    }
}
