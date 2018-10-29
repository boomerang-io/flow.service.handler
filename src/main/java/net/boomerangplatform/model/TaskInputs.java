
package net.boomerangplatform.model;

import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties
public class TaskInputs {

	@JsonProperty
    private Map<String, String> properties = new HashMap<String, String>();

    public Map<String, String> getProperties() {
        return this.properties;
    }

    public void setProperty(String name, String value) {
        this.properties.put(name, value);
    }

}
