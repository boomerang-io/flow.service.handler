
package net.boomerangplatform.model.argo;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.google.gson.annotations.SerializedName;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({"kubectl.kubernetes.io/last-applied-configuration"})
public class Annotations extends GeneralProperties {

  @JsonProperty("kubectl.kubernetes.io/last-applied-configuration")
  @SerializedName("kubectl.kubernetes.io/last-applied-configuration")
  private String kubectlKubernetesIoLastAppliedConfiguration;

  public Annotations() {
    // Do nothing
  }

  public String getKubectlKubernetesIoLastAppliedConfiguration() {
    return kubectlKubernetesIoLastAppliedConfiguration;
  }

  public void setKubectlKubernetesIoLastAppliedConfiguration(
      String kubectlKubernetesIoLastAppliedConfiguration) {
    this.kubectlKubernetesIoLastAppliedConfiguration = kubectlKubernetesIoLastAppliedConfiguration;
  }

}
