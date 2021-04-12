package net.boomerangplatform.service;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import net.boomerangplatform.kube.service.NewKubeServiceImpl;
import net.boomerangplatform.model.TaskDeletionEnum;

/*
 * Implement asynchronous delete after a period of time to ensure
 * that logs can be retrieved / ingested from quick running tasks
 */
@Service
public class DeleteServiceImpl implements DeleteService {

  private static final Logger LOGGER = LogManager.getLogger(DeleteServiceImpl.class);
  
  private static final long sleep = 1000;
  
  @Autowired
  private NewKubeServiceImpl kubeService;

  @Override
  @Async
  public void deleteJob(TaskDeletionEnum taskDeletion, String workflowId, String workflowActivityId, String taskId, String taskActivityId) {
    try {
      Thread.sleep(sleep);
    } catch (InterruptedException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
//    kubeService.deleteJob(taskDeletion,workflowId,workflowActivityId, taskId, taskActivityId);
  }
  
//  @Override
//  @Async
//  public void deleteWorkspacePVC(String workspaceId) {
//    try {
//      LOGGER.debug("Inside deleting service");
//      Thread.sleep(sleep);
//    } catch (InterruptedException e) {
//      // TODO Auto-generated catch block
//      e.printStackTrace();
//    }
//    kubeService.deleteWorkspacePVC(workspaceId);
//  }
}
