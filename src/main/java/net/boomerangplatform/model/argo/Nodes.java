
package net.boomerangplatform.model.argo;

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
    "bmrg-flow-1",
    "bmrg-flow-1-15782439",
    "bmrg-flow-1-4081481",
    "bmrg-flow-1-548609519",
    "bmrg-flow-1-615867090"
})
public class Nodes {

    @JsonProperty("bmrg-flow-1")
    private BmrgFlow1 bmrgFlow1;
    @JsonProperty("bmrg-flow-1-15782439")
    private BmrgFlow115782439 bmrgFlow115782439;
    @JsonProperty("bmrg-flow-1-4081481")
    private BmrgFlow14081481 bmrgFlow14081481;
    @JsonProperty("bmrg-flow-1-548609519")
    private BmrgFlow1548609519 bmrgFlow1548609519;
    @JsonProperty("bmrg-flow-1-615867090")
    private BmrgFlow1615867090 bmrgFlow1615867090;
    @JsonIgnore
    private Map<String, Object> additionalProperties = new HashMap<String, Object>();

    @JsonProperty("bmrg-flow-1")
    public BmrgFlow1 getBmrgFlow1() {
        return bmrgFlow1;
    }

    @JsonProperty("bmrg-flow-1")
    public void setBmrgFlow1(BmrgFlow1 bmrgFlow1) {
        this.bmrgFlow1 = bmrgFlow1;
    }

    @JsonProperty("bmrg-flow-1-15782439")
    public BmrgFlow115782439 getBmrgFlow115782439() {
        return bmrgFlow115782439;
    }

    @JsonProperty("bmrg-flow-1-15782439")
    public void setBmrgFlow115782439(BmrgFlow115782439 bmrgFlow115782439) {
        this.bmrgFlow115782439 = bmrgFlow115782439;
    }

    @JsonProperty("bmrg-flow-1-4081481")
    public BmrgFlow14081481 getBmrgFlow14081481() {
        return bmrgFlow14081481;
    }

    @JsonProperty("bmrg-flow-1-4081481")
    public void setBmrgFlow14081481(BmrgFlow14081481 bmrgFlow14081481) {
        this.bmrgFlow14081481 = bmrgFlow14081481;
    }

    @JsonProperty("bmrg-flow-1-548609519")
    public BmrgFlow1548609519 getBmrgFlow1548609519() {
        return bmrgFlow1548609519;
    }

    @JsonProperty("bmrg-flow-1-548609519")
    public void setBmrgFlow1548609519(BmrgFlow1548609519 bmrgFlow1548609519) {
        this.bmrgFlow1548609519 = bmrgFlow1548609519;
    }

    @JsonProperty("bmrg-flow-1-615867090")
    public BmrgFlow1615867090 getBmrgFlow1615867090() {
        return bmrgFlow1615867090;
    }

    @JsonProperty("bmrg-flow-1-615867090")
    public void setBmrgFlow1615867090(BmrgFlow1615867090 bmrgFlow1615867090) {
        this.bmrgFlow1615867090 = bmrgFlow1615867090;
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
