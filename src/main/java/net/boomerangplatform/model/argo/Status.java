
package net.boomerangplatform.model.argo;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({"finishedAt", "nodes", "phase", "startedAt"})
public class Status extends GeneralProperties {

  private String finishedAt;
  private Nodes nodes;
  private String phase;
  private String startedAt;

  public String getFinishedAt() {
    return finishedAt;
  }

  public void setFinishedAt(String finishedAt) {
    this.finishedAt = finishedAt;
  }

  public Nodes getNodes() {
    return nodes;
  }

  public void setNodes(Nodes nodes) {
    this.nodes = nodes;
  }

  public String getPhase() {
    return phase;
  }

  public void setPhase(String phase) {
    this.phase = phase;
  }

  public String getStartedAt() {
    return startedAt;
  }

  public void setStartedAt(String startedAt) {
    this.startedAt = startedAt;
  }

}
