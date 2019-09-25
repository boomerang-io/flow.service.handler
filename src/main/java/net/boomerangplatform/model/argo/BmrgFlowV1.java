
package net.boomerangplatform.model.argo;

import static net.boomerangplatform.util.ListUtil.sanityNullList;
import java.util.List;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({"children", "displayName", "finishedAt", "id", "name", "outboundNodes", "phase",
    "startedAt", "templateName", "type"})
public class BmrgFlowV1 extends BmrgFlow {
  private List<String> children;
  private List<String> outboundNodes;
  private String templateName;

  public BmrgFlowV1() {
    // Do nothing
  }

  public List<String> getChildren() {
    return sanityNullList(children);
  }

  public void setChildren(List<String> children) {
    this.children = sanityNullList(children);
  }


  public List<String> getOutboundNodes() {
    return sanityNullList(outboundNodes);
  }

  public void setOutboundNodes(List<String> outboundNodes) {
    this.outboundNodes = sanityNullList(outboundNodes);
  }


  public String getTemplateName() {
    return templateName;
  }

  public void setTemplateName(String templateName) {
    this.templateName = templateName;
  }

}
