
package net.boomerangplatform.model.argo;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({"boundaryID", "displayName", "finishedAt", "id", "name", "outputs", "phase",
    "startedAt", "templateName", "type"})
public class BmrgFlowV2 extends BmrgFlow {

  private String boundaryID;
  private Outputs outputs;
  private String templateName;

  public BmrgFlowV2() {
    // Do nothing
  }

  public String getBoundaryID() {
    return boundaryID;
  }

  public void setBoundaryID(String boundaryID) {
    this.boundaryID = boundaryID;
  }

  public Outputs getOutputs() {
    return outputs;
  }

  public void setOutputs(Outputs outputs) {
    this.outputs = outputs;
  }

  public String getTemplateName() {
    return templateName;
  }

  public void setTemplateName(String templateName) {
    this.templateName = templateName;
  }
}
