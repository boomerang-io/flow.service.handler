package net.boomerangplatform.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import net.boomerangplatform.kube.service.KubeServiceImpl;
import net.boomerangplatform.model.TaskDeletionEnum;

@Service
public class DeleteServiceImpl implements DeleteService {
  
  @Autowired
  private KubeServiceImpl kubeService;

  /*
   * Implement asynchronous delete after a period of time to ensure
   * that logs can be retrieved / ingested from quick running tasks
   */
  @Override
  @Async
  public void deleteJob(TaskDeletionEnum taskDeletion, String workflowId, String workflowActivityId, String taskId) {
    try {
      Thread.sleep(10000);
    } catch (InterruptedException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
    kubeService.deleteJob(taskDeletion,workflowId,workflowActivityId, taskId);
  }
}
