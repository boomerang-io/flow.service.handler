package net.boomerangplatform.model;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties
public class Task {
	
	@JsonProperty("workflowName")
    private String workflowName;
	
	@JsonProperty("workflowId")
    private String workflowId;
	
	@JsonProperty("workflowActivityId")
    private String workflowActivityId;

	@JsonProperty("taskName")
    private String taskName;
	
	@JsonProperty("taskId")
    private String taskId;
	
	@JsonProperty("inputs")
	private TaskProperties inputs;
	
	@JsonProperty("arguments")
    private List<String> arguments;

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

	public String getTaskId() {
		return taskId;
	}

	public void setTaskId(String taskId) {
		this.taskId = taskId;
	}

	public String getTaskName() {
		return taskName;
	}

	public void setTaskName(String taskName) {
		this.taskName = taskName;
	}

	public TaskProperties getInputs() {
		return inputs;
	}

	public void setInputs(TaskProperties inputs) {
		this.inputs = inputs;
	}

	public List<String> getArguments() {
		return arguments;
	}

	public void setArguments(List<String> arguments) {
		this.arguments = arguments;
	}
	
	public void setArgument(String argument) {
        this.arguments.add(argument);
    }
}
