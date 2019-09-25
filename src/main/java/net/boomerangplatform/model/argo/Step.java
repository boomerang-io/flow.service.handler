
package net.boomerangplatform.model.argo;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({"arguments", "name", "template"})
public class Step extends GeneralProperties {

  private Arguments arguments;
  private String name;
  private String template;

  public Arguments getArguments() {
    return arguments;
  }

  public void setArguments(Arguments arguments) {
    this.arguments = arguments;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getTemplate() {
    return template;
  }

  public void setTemplate(String template) {
    this.template = template;
  }

}
