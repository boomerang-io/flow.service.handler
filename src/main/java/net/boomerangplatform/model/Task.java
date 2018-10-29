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
	
	@JsonProperty("taskId")
    private String taskId;
	
	@JsonProperty("inputs")
	private TaskInputs inputs;
	
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

	public String getTaskId() {
		return taskId;
	}

	public void setTaskId(String taskId) {
		this.taskId = taskId;
	}

	public TaskInputs getInputs() {
		return inputs;
	}

	public void setInputs(TaskInputs inputs) {
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
