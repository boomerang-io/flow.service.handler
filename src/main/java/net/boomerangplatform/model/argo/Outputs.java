
package net.boomerangplatform.model.argo;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({"result"})
public class Outputs extends GeneralProperties {

  private String result;

  public String getResult() {
    return result;
  }

  public void setResult(String result) {
    this.result = result;
  }

}
