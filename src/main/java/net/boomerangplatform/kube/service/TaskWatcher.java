package net.boomerangplatform.kube.service;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import io.fabric8.knative.internal.pkg.apis.Condition;
import io.fabric8.kubernetes.client.Watcher;
import io.fabric8.kubernetes.client.WatcherException;
import io.fabric8.tekton.pipeline.v1beta1.TaskRun;
import io.fabric8.tekton.pipeline.v1beta1.TaskRunResult;

public class TaskWatcher implements Watcher<TaskRun>{

  private static final Logger LOGGER = LogManager.getLogger(TaskWatcher.class);
  
  private final CountDownLatch latch;
  private Condition condition;
  private List<TaskRunResult> results;
  
  public TaskWatcher(CountDownLatch latch) {
    this.latch = latch;
  }
  
  public Condition getCondition() {
    return condition;
  }
  
  public List<TaskRunResult> getResults() {
    return results;
  }

  @Override
  /*
   * Process the Watcher Events and return as a Condition result object
   * - Processes a delete event occurred by external source such as CLI
   * 
   * Reference(s):
   * - https://tekton.dev/vault/pipelines-v0.14.3/taskruns/#monitoring-execution-status
   * - 
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
       results = resource.getStatus().getTaskResults();
       LOGGER.info(results.toString());
       switch (action.name()) {
         case "DELETED":
           if ("Unknown".equals(resource.getStatus().getConditions().get(0).getStatus())) {
             LOGGER.info(" Task Cancelled Externally. Adjusting status");
             condition = resource.getStatus().getConditions().get(0);
             condition.setStatus("False");
             condition.setReason("TaskRunCancelled");
             condition.setMessage("The TaskRun was cancelled successfully.");
             latch.countDown();
           }
       }
      switch (taskStatus) {
        case "False":
          condition = resource.getStatus().getConditions().get(0);
          latch.countDown();
        case "True":
          condition = resource.getStatus().getConditions().get(0);
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
