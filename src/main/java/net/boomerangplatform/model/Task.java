package net.boomerangplatform.model;

import static net.boomerangplatform.util.ListUtil.sanityNullList;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties
public class Task {

  private String workflowName;

  private String workflowId;

  private String workflowActivityId;

  private String taskName;

  private String taskId;

  @JsonProperty
  private Map<String, String> properties = new HashMap<>();

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

  public void setProperties(Map<String, String> properties) {
    this.properties = properties;
  }

  public Map<String, String> getProperties() {
    return this.properties;
  }

  public void setProperty(String name, String value) {
    this.properties.put(name, value);
  }

  public List<String> getArguments() {
    return sanityNullList(arguments);
  }

  public void setArguments(List<String> arguments) {
    this.arguments = sanityNullList(arguments);
  }

  public void setArgument(String argument) {
    this.arguments.add(argument);
  }
}
