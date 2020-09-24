package net.boomerangplatform.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import net.boomerangplatform.kube.service.FlowKubeServiceImpl;
import net.boomerangplatform.model.TaskDeletion;

@Service
@Profile({"live", "local"})
public class FlowDeleteServiceImpl implements FlowDeleteService {
  
  @Autowired
  private FlowKubeServiceImpl kubeService;

  @Override
  @Async
  public void deleteJob(TaskDeletion taskDeletion, String workflowId, String workflowActivityId, String taskId) {
    try {
      Thread.sleep(10000);
    } catch (InterruptedException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
    kubeService.deleteJob(taskDeletion,workflowId,workflowActivityId, taskId);
  }
}
