package net.boomerangplatform.kube.service;

import java.util.concurrent.CountDownLatch;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import io.fabric8.knative.internal.pkg.apis.Condition;
import io.fabric8.kubernetes.client.Watcher;
import io.fabric8.kubernetes.client.WatcherException;
import io.fabric8.tekton.pipeline.v1beta1.TaskRun;

public class TaskWatcher implements Watcher<TaskRun>{

  private static final Logger LOGGER = LogManager.getLogger(TaskWatcher.class);
  
  private final CountDownLatch latch;
  private Condition result;
  
  public TaskWatcher(CountDownLatch latch) {
    this.latch = latch;
  }
  
  public Condition getResult() {
    return result;
  }

  @Override
  /*
   * Process the Watcher Events and return as a Condition result object
   * - Processes a delete event occurred by external source such as CLI
   * 
   * Reference: https://tekton.dev/vault/pipelines-v0.14.3/taskruns/#monitoring-execution-status
   */
  public void eventReceived(Action action, TaskRun resource) {
    LOGGER.info("Watch event received {}: {}", action.name(),
        resource.getMetadata().getName());
    if (resource.getStatus().getConditions() != null) {
      String taskStatus = resource.getStatus().getConditions().get(0).getStatus();
      LOGGER.info("TaskRun Name: " + resource.getMetadata().getName() + ",\n  Start Time: " + resource.getStatus().getStartTime() + ",\n  Status: "
          + taskStatus);
       for (Condition condition : resource.getStatus().getConditions()) {
         LOGGER.info(" Task Status Condition: " + condition.toString());
       }
       LOGGER.info(resource.getStatus().getTaskResults().toString());
       switch (action.name()) {
         case "DELETED":
           if ("Unknown".equals(resource.getStatus().getConditions().get(0).getStatus())) {
             LOGGER.info(" Task Cancelled Externally. Adjusting status");
             result = resource.getStatus().getConditions().get(0);
             result.setStatus("False");
             result.setReason("TaskRunCancelled");
             result.setMessage("The TaskRun was cancelled successfully.");
             latch.countDown();
           }
       }
      switch (taskStatus) {
        case "False":
          result = resource.getStatus().getConditions().get(0);
          latch.countDown();
        case "True":
          result = resource.getStatus().getConditions().get(0);
          latch.countDown();
      }
    }
  }

  @Override
  public void onClose(WatcherException e) {
    LOGGER.error("Watch error received: {}", e.getMessage(), e);
    // Cause the pod to restart
    System.exit(1);
  }

}
