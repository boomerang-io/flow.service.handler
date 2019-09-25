
package net.boomerangplatform.model.argo;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({"name"})
public class ImagePullSecret extends GeneralProperties {

  private String name;

  public ImagePullSecret() {
    // Do nothing
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

}
