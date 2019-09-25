
package net.boomerangplatform.model.argo;

import static net.boomerangplatform.util.ListUtil.sanityNullList;
import java.util.List;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({"inputs", "metadata", "name", "outputs", "steps", "script"})
public class Template extends GeneralProperties {

  private Inputs inputs;
  private GeneralProperties metadata;
  private String name;
  private GeneralProperties outputs;
  private List<List<Step>> steps;
  private Script script;

  public Inputs getInputs() {
    return inputs;
  }

  public void setInputs(Inputs inputs) {
    this.inputs = inputs;
  }

  public GeneralProperties getMetadata() {
    return metadata;
  }

  public void setMetadata(GeneralProperties metadata) {
    this.metadata = metadata;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public GeneralProperties getOutputs() {
    return outputs;
  }

  public void setOutputs(GeneralProperties outputs) {
    this.outputs = outputs;
  }

  public List<List<Step>> getSteps() {
    return sanityNullList(steps);
  }

  public void setSteps(List<List<Step>> steps) {
    this.steps = sanityNullList(steps);
  }

  public Script getScript() {
    return script;
  }

  public void setScript(Script script) {
    this.script = script;
  }

}
