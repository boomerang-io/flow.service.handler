
package net.boomerangplatform.model;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
    "inputs",
    "metadata",
    "name",
    "outputs",
    "steps",
    "script"
})
public class Template {

    @JsonProperty("inputs")
    private Inputs inputs;
    @JsonProperty("metadata")
    private Metadata_ metadata;
    @JsonProperty("name")
    private String name;
    @JsonProperty("outputs")
    private Outputs outputs;
    @JsonProperty("steps")
    private List<List<Step>> steps = null;
    @JsonProperty("script")
    private Script script;
    @JsonIgnore
    private Map<String, Object> additionalProperties = new HashMap<String, Object>();

    @JsonProperty("inputs")
    public Inputs getInputs() {
        return inputs;
    }

    @JsonProperty("inputs")
    public void setInputs(Inputs inputs) {
        this.inputs = inputs;
    }

    @JsonProperty("metadata")
    public Metadata_ getMetadata() {
        return metadata;
    }

    @JsonProperty("metadata")
    public void setMetadata(Metadata_ metadata) {
        this.metadata = metadata;
    }

    @JsonProperty("name")
    public String getName() {
        return name;
    }

    @JsonProperty("name")
    public void setName(String name) {
        this.name = name;
    }

    @JsonProperty("outputs")
    public Outputs getOutputs() {
        return outputs;
    }

    @JsonProperty("outputs")
    public void setOutputs(Outputs outputs) {
        this.outputs = outputs;
    }

    @JsonProperty("steps")
    public List<List<Step>> getSteps() {
        return steps;
    }

    @JsonProperty("steps")
    public void setSteps(List<List<Step>> steps) {
        this.steps = steps;
    }

    @JsonProperty("script")
    public Script getScript() {
        return script;
    }

    @JsonProperty("script")
    public void setScript(Script script) {
        this.script = script;
    }

    @JsonAnyGetter
    public Map<String, Object> getAdditionalProperties() {
        return this.additionalProperties;
    }

    @JsonAnySetter
    public void setAdditionalProperty(String name, Object value) {
        this.additionalProperties.put(name, value);
    }

}
