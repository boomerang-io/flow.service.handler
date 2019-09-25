
package net.boomerangplatform.model.argo;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.google.gson.annotations.SerializedName;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({"bmrg-flow-1", "bmrg-flow-1-15782439", "bmrg-flow-1-4081481",
    "bmrg-flow-1-548609519", "bmrg-flow-1-615867090"})
public class Nodes extends GeneralProperties {

  @JsonProperty("bmrg-flow-1")
  @SerializedName("bmrg-flow-1")
  private BmrgFlowV1 bmrgFlow1;
  @JsonProperty("bmrg-flow-1-15782439")
  @SerializedName("bmrg-flow-1-15782439")
  private BmrgFlowV2 bmrgFlow115782439;
  @JsonProperty("bmrg-flow-1-4081481")
  @SerializedName("bmrg-flow-1-4081481")
  private BmrgFlowV3 bmrgFlow14081481;
  @JsonProperty("bmrg-flow-1-548609519")
  @SerializedName("bmrg-flow-1-548609519")
  private BmrgFlowV4 bmrgFlow1548609519;
  @JsonProperty("bmrg-flow-1-615867090")
  @SerializedName("bmrg-flow-1-615867090")
  private BmrgFlowV4 bmrgFlow1615867090;

  public Nodes() {
    // Do nothing
  }

  public BmrgFlowV1 getBmrgFlow1() {
    return bmrgFlow1;
  }

  public void setBmrgFlow1(BmrgFlowV1 bmrgFlow1) {
    this.bmrgFlow1 = bmrgFlow1;
  }

  public BmrgFlowV2 getBmrgFlow115782439() {
    return bmrgFlow115782439;
  }

  public void setBmrgFlow115782439(BmrgFlowV2 bmrgFlow115782439) {
    this.bmrgFlow115782439 = bmrgFlow115782439;
  }

  public BmrgFlowV3 getBmrgFlow14081481() {
    return bmrgFlow14081481;
  }

  public void setBmrgFlow14081481(BmrgFlowV3 bmrgFlow14081481) {
    this.bmrgFlow14081481 = bmrgFlow14081481;
  }

  public BmrgFlowV4 getBmrgFlow1548609519() {
    return bmrgFlow1548609519;
  }

  public void setBmrgFlow1548609519(BmrgFlowV4 bmrgFlow1548609519) {
    this.bmrgFlow1548609519 = bmrgFlow1548609519;
  }

  public BmrgFlowV4 getBmrgFlow1615867090() {
    return bmrgFlow1615867090;
  }

  public void setBmrgFlow1615867090(BmrgFlowV4 bmrgFlow1615867090) {
    this.bmrgFlow1615867090 = bmrgFlow1615867090;
  }

}
