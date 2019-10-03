package net.boomerangplatform.model;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties
public class Workflow {
	
	@JsonProperty("workflowName")
    private String workflowName;
	
	@JsonProperty("workflowId")
    private String workflowId;
	
	@JsonProperty("workflowActivityId")
    private String workflowActivityId;
	
	@JsonProperty("storage")
    private WorkflowStorage storage;
	
	@JsonProperty("inputs")
    private Map<String, String> inputs;

	public String getWorkflowName() {
		return workflowName;
	}

	public void setWorkflowName(String workflowName) {
		this.workflowName = workflowName;
	}

	public String getWorkflowId() {
		return workflowId;
	}

	public void setWorkflowId(String workflowId) {
		this.workflowId = workflowId;
	}

	public String getWorkflowActivityId() {
		return workflowActivityId;
	}

	public void setWorkflowActivityId(String workflowActivityId) {
		this.workflowActivityId = workflowActivityId;
	}

	public WorkflowStorage getWorkflowStorage() {
		return storage;
	}

	public void setWorkflowStorage(WorkflowStorage storage) {
		this.storage = storage;
	}

	public Map<String, String> getInputs() {
		return inputs;
	}

	public void setInputs(Map<String, String> inputs) {
		this.inputs = inputs;
	}
}
