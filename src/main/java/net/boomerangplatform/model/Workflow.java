package net.boomerangplatform.model;

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

}
