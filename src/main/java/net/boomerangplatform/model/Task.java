package net.boomerangplatform.model;

import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties
public class Task {
	
	@JsonProperty("workflowName")
    private String workflowName;
	
	@JsonProperty("workflowID")
    private String workflowID;
	
	@JsonProperty("inputProperties")
	private Map<String, String> inputProperties = new HashMap<String, String>();


	public String getWorkflowName() {
		return workflowName;
	}

	public void setWorkflowName(String workflowName) {
		this.workflowName = workflowName;
	}

	public String getWorkflowID() {
		return workflowID;
	}

	public void setWorkflowID(String workflowID) {
		this.workflowID = workflowID;
	}

	public Map<String, String> getInputProperties() {
		return inputProperties;
	}

	public void setInputProperties(Map<String, String> inputProperties) {
		this.inputProperties = inputProperties;
	}
	
    public void setInputProperty(String name, String value) {
        this.inputProperties.put(name, value);
    }
}
