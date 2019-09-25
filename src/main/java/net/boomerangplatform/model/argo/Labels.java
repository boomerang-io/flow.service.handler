
package net.boomerangplatform.model.argo;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.google.gson.annotations.SerializedName;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({"workflows.argoproj.io/completed", "workflows.argoproj.io/phase"})
public class Labels extends GeneralProperties {

  @JsonProperty("workflows.argoproj.io/completed")
  @SerializedName("workflows.argoproj.io/completed")
  private String workflowsArgoprojIoCompleted;
  @JsonProperty("workflows.argoproj.io/phase")
  @SerializedName("workflows.argoproj.io/phase")
  private String workflowsArgoprojIoPhase;

  public Labels() {
    // Do nothing
  }

  public String getWorkflowsArgoprojIoCompleted() {
    return workflowsArgoprojIoCompleted;
  }

  public void setWorkflowsArgoprojIoCompleted(String workflowsArgoprojIoCompleted) {
    this.workflowsArgoprojIoCompleted = workflowsArgoprojIoCompleted;
  }

  public String getWorkflowsArgoprojIoPhase() {
    return workflowsArgoprojIoPhase;
  }

  public void setWorkflowsArgoprojIoPhase(String workflowsArgoprojIoPhase) {
    this.workflowsArgoprojIoPhase = workflowsArgoprojIoPhase;
  }
}
