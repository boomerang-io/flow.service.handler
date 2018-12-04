
package net.boomerangplatform.model.argo;

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
    "arguments",
    "entrypoint",
    "imagePullSecrets",
    "templates"
})
public class Spec {

    @JsonProperty("arguments")
    private Arguments arguments;
    @JsonProperty("entrypoint")
    private String entrypoint;
    @JsonProperty("imagePullSecrets")
    private List<ImagePullSecret> imagePullSecrets = null;
    @JsonProperty("templates")
    private List<Template> templates = null;
    @JsonIgnore
    private Map<String, Object> additionalProperties = new HashMap<String, Object>();

    @JsonProperty("arguments")
    public Arguments getArguments() {
        return arguments;
    }

    @JsonProperty("arguments")
    public void setArguments(Arguments arguments) {
        this.arguments = arguments;
    }

    @JsonProperty("entrypoint")
    public String getEntrypoint() {
        return entrypoint;
    }

    @JsonProperty("entrypoint")
    public void setEntrypoint(String entrypoint) {
        this.entrypoint = entrypoint;
    }

    @JsonProperty("imagePullSecrets")
    public List<ImagePullSecret> getImagePullSecrets() {
        return imagePullSecrets;
    }

    @JsonProperty("imagePullSecrets")
    public void setImagePullSecrets(List<ImagePullSecret> imagePullSecrets) {
        this.imagePullSecrets = imagePullSecrets;
    }

    @JsonProperty("templates")
    public List<Template> getTemplates() {
        return templates;
    }

    @JsonProperty("templates")
    public void setTemplates(List<Template> templates) {
        this.templates = templates;
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
