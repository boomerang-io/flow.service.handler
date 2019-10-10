package net.boomerangplatform.model;

import java.util.Map;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties
public class Workflow {

  private String workflowName;

  private String workflowId;

  private String workflowActivityId;

  private WorkflowStorage storage;

  private Map<String, String> inputs;

  public String getWorkflowName() {
    return workflowName;
  }

  public void setWorkflowName(String workflowName) {
    this.workflowName = workflowName;
  }

  public String getWorkflowId() {
    return workflowId;
  }

  public void setWorkflowId(String workflowId) {
    this.workflowId = workflowId;
  }

  public String getWorkflowActivityId() {
    return workflowActivityId;
  }

  public void setWorkflowActivityId(String workflowActivityId) {
    this.workflowActivityId = workflowActivityId;
  }

  public WorkflowStorage getWorkflowStorage() {
    return storage;
  }

  public void setWorkflowStorage(WorkflowStorage storage) {
    this.storage = storage;
  }

  public Map<String, String> getProperties() {
    return inputs;
  }

  public void setInputs(Map<String, String> inputs) {
    this.inputs = inputs;
  }
}
