
package net.boomerangplatform.model.argo;

import static net.boomerangplatform.util.ListUtil.sanityNullList;
import java.util.List;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({"arguments", "entrypoint", "imagePullSecrets", "templates"})
public class Spec extends GeneralProperties {

  private Arguments arguments;
  private String entrypoint;
  private List<ImagePullSecret> imagePullSecrets;
  private List<Template> templates;

  public Arguments getArguments() {
    return arguments;
  }

  public void setArguments(Arguments arguments) {
    this.arguments = arguments;
  }

  public String getEntrypoint() {
    return entrypoint;
  }

  public void setEntrypoint(String entrypoint) {
    this.entrypoint = entrypoint;
  }

  public List<ImagePullSecret> getImagePullSecrets() {
    return sanityNullList(imagePullSecrets);
  }

  public void setImagePullSecrets(List<ImagePullSecret> imagePullSecrets) {
    this.imagePullSecrets = sanityNullList(imagePullSecrets);
  }

  public List<Template> getTemplates() {
    return sanityNullList(templates);
  }

  public void setTemplates(List<Template> templates) {
    this.templates = sanityNullList(templates);
  }

}
