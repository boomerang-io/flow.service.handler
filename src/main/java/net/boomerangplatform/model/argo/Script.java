
package net.boomerangplatform.model.argo;

import static net.boomerangplatform.util.ListUtil.sanityNullList;
import java.util.List;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({"command", "image", "name", "resources", "source"})
public class Script extends GeneralProperties {

  private List<String> command;
  private String image;
  private String name;
  private GeneralProperties resources;
  private String source;

  public List<String> getCommand() {
    return sanityNullList(command);
  }

  public void setCommand(List<String> command) {
    this.command = sanityNullList(command);
  }

  public String getImage() {
    return image;
  }

  public void setImage(String image) {
    this.image = image;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public GeneralProperties getResources() {
    return resources;
  }

  public void setResources(GeneralProperties resources) {
    this.resources = resources;
  }

  public String getSource() {
    return source;
  }

  public void setSource(String source) {
    this.source = source;
  }

}
