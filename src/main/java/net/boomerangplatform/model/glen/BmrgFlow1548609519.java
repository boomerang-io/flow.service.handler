
package net.boomerangplatform.model.glen;

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
    "boundaryID",
    "children",
    "displayName",
    "finishedAt",
    "id",
    "name",
    "phase",
    "startedAt",
    "type"
})
public class BmrgFlow1548609519 {

    @JsonProperty("boundaryID")
    private String boundaryID;
    @JsonProperty("children")
    private List<String> children = null;
    @JsonProperty("displayName")
    private String displayName;
    @JsonProperty("finishedAt")
    private String finishedAt;
    @JsonProperty("id")
    private String id;
    @JsonProperty("name")
    private String name;
    @JsonProperty("phase")
    private String phase;
    @JsonProperty("startedAt")
    private String startedAt;
    @JsonProperty("type")
    private String type;
    @JsonIgnore
    private Map<String, Object> additionalProperties = new HashMap<String, Object>();

    @JsonProperty("boundaryID")
    public String getBoundaryID() {
        return boundaryID;
    }

    @JsonProperty("boundaryID")
    public void setBoundaryID(String boundaryID) {
        this.boundaryID = boundaryID;
    }

    @JsonProperty("children")
    public List<String> getChildren() {
        return children;
    }

    @JsonProperty("children")
    public void setChildren(List<String> children) {
        this.children = children;
    }

    @JsonProperty("displayName")
    public String getDisplayName() {
        return displayName;
    }

    @JsonProperty("displayName")
    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    @JsonProperty("finishedAt")
    public String getFinishedAt() {
        return finishedAt;
    }

    @JsonProperty("finishedAt")
    public void setFinishedAt(String finishedAt) {
        this.finishedAt = finishedAt;
    }

    @JsonProperty("id")
    public String getId() {
        return id;
    }

    @JsonProperty("id")
    public void setId(String id) {
        this.id = id;
    }

    @JsonProperty("name")
    public String getName() {
        return name;
    }

    @JsonProperty("name")
    public void setName(String name) {
        this.name = name;
    }

    @JsonProperty("phase")
    public String getPhase() {
        return phase;
    }

    @JsonProperty("phase")
    public void setPhase(String phase) {
        this.phase = phase;
    }

    @JsonProperty("startedAt")
    public String getStartedAt() {
        return startedAt;
    }

    @JsonProperty("startedAt")
    public void setStartedAt(String startedAt) {
        this.startedAt = startedAt;
    }

    @JsonProperty("type")
    public String getType() {
        return type;
    }

    @JsonProperty("type")
    public void setType(String type) {
        this.type = type;
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
