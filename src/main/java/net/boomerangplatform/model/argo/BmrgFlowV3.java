
package net.boomerangplatform.model.argo;

import static net.boomerangplatform.util.ListUtil.sanityNullList;
import java.util.List;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({"boundaryID", "children", "displayName", "finishedAt", "id", "name", "outputs",
    "phase", "startedAt", "templateName", "type"})
public class BmrgFlowV3 extends BmrgFlow {

  private String boundaryID;
  private List<String> children;
  private Outputs outputs;
  private String templateName;

  public BmrgFlowV3() {
    // Do nothing
  }

  public String getBoundaryID() {
    return boundaryID;
  }

  public void setBoundaryID(String boundaryID) {
    this.boundaryID = boundaryID;
  }

  public List<String> getChildren() {
    return sanityNullList(children);
  }

  public void setChildren(List<String> children) {
    this.children = sanityNullList(children);
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
