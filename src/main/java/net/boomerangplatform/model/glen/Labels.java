
package net.boomerangplatform.model.glen;

import java.util.HashMap;
import java.util.Map;
import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
    "workflows.argoproj.io/completed",
    "workflows.argoproj.io/phase"
})
public class Labels {

    @JsonProperty("workflows.argoproj.io/completed")
    private String workflowsArgoprojIoCompleted;
    @JsonProperty("workflows.argoproj.io/phase")
    private String workflowsArgoprojIoPhase;
    @JsonIgnore
    private Map<String, Object> additionalProperties = new HashMap<String, Object>();

    @JsonProperty("workflows.argoproj.io/completed")
    public String getWorkflowsArgoprojIoCompleted() {
        return workflowsArgoprojIoCompleted;
    }

    @JsonProperty("workflows.argoproj.io/completed")
    public void setWorkflowsArgoprojIoCompleted(String workflowsArgoprojIoCompleted) {
        this.workflowsArgoprojIoCompleted = workflowsArgoprojIoCompleted;
    }

    @JsonProperty("workflows.argoproj.io/phase")
    public String getWorkflowsArgoprojIoPhase() {
        return workflowsArgoprojIoPhase;
    }

    @JsonProperty("workflows.argoproj.io/phase")
    public void setWorkflowsArgoprojIoPhase(String workflowsArgoprojIoPhase) {
        this.workflowsArgoprojIoPhase = workflowsArgoprojIoPhase;
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
