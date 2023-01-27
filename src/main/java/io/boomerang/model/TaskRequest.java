package io.boomerang.model;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonSubTypes.Type;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.boomerang.model.ref.RunParam;
import io.boomerang.model.ref.RunResult;
import io.boomerang.model.ref.TaskDeletionEnum;
import io.boomerang.model.ref.TaskEnvVar;
import io.boomerang.model.ref.TaskWorkspace;

@JsonTypeInfo(
		  use = JsonTypeInfo.Id.NAME, 
		  include = JsonTypeInfo.As.PROPERTY, 
		  property = "taskType")
		@JsonSubTypes({ 
		  @Type(value = TaskCustom.class, name = "custom"), 
		  @Type(value = TaskTemplate.class, name = "template")
		})
@JsonIgnoreProperties
public abstract class TaskRequest {
  private String workflowRef;
  private String workflowRunRef;
  private String taskRunRef;
  private String taskName;
  private String image;
  private List<String> command;
  private String workingDir;
  private String script;
  private Map<String, String> labels = new HashMap<>();
  private List<RunParam> params = new LinkedList<>();
  private List<TaskEnvVar> envs;
  private List<RunResult> results = new LinkedList<>();
  private List<String> arguments;
  private List<TaskWorkspace> workspaces;
  private Boolean debug = false;
  private int timeout;
  private TaskDeletionEnum deletion;
   
  @Override
  public String toString() {
    return "TaskRequest [workflowRef=" + workflowRef + ", workflowRunRef=" + workflowRunRef
        + ", taskRunRef=" + taskRunRef + ", taskName=" + taskName + ", image=" + image
        + ", command=" + command + ", workingDir=" + workingDir + ", script=" + script + ", labels="
        + labels + ", params=" + params + ", envs=" + envs + ", results=" + results + ", arguments="
        + arguments + ", workspaces=" + workspaces + ", debug=" + debug + ", timeout=" + timeout
        + ", deletion=" + deletion + "]";
  }
  public String getWorkflowRef() {
    return workflowRef;
  }
  public void setWorkflowRef(String workflowRef) {
    this.workflowRef = workflowRef;
  }
  public String getWorkflowRunRef() {
    return workflowRunRef;
  }
  public void setWorkflowRunRef(String workflowRunRef) {
    this.workflowRunRef = workflowRunRef;
  }
  public String getTaskRunRef() {
    return taskRunRef;
  }
  public void setTaskRunRef(String taskRunRef) {
    this.taskRunRef = taskRunRef;
  }
  public String getTaskName() {
    return taskName;
  }
  public void setTaskName(String taskName) {
    this.taskName = taskName;
  }
  public String getImage() {
    return image;
  }
  public void setImage(String image) {
    this.image = image;
  }
  public List<String> getCommand() {
    return command;
  }
  public void setCommand(List<String> command) {
    this.command = command;
  }
  public String getWorkingDir() {
    return workingDir;
  }
  public void setWorkingDir(String workingDir) {
    this.workingDir = workingDir;
  }
  public String getScript() {
    return script;
  }
  public void setScript(String script) {
    this.script = script;
  }
  public Map<String, String> getLabels() {
    return labels;
  }
  public void setLabels(Map<String, String> labels) {
    this.labels = labels;
  }
  public List<RunParam> getParams() {
    return params;
  }
  public void setParams(List<RunParam> params) {
    this.params = params;
  }
  public List<TaskEnvVar> getEnvs() {
    return envs;
  }
  public void setEnvs(List<TaskEnvVar> envs) {
    this.envs = envs;
  }
  public List<RunResult> getResults() {
    return results;
  }
  public void setResults(List<RunResult> results) {
    this.results = results;
  }
  public List<String> getArguments() {
    return arguments;
  }
  public void setArguments(List<String> arguments) {
    this.arguments = arguments;
  }
  public List<TaskWorkspace> getWorkspaces() {
    return workspaces;
  }
  public void setWorkspaces(List<TaskWorkspace> workspaces) {
    this.workspaces = workspaces;
  }
  public Boolean getDebug() {
    return debug;
  }
  public void setDebug(Boolean debug) {
    this.debug = debug;
  }
  public int getTimeout() {
    return timeout;
  }
  public void setTimeout(int timeout) {
    this.timeout = timeout;
  }
  public TaskDeletionEnum getDeletion() {
    return deletion;
  }
  public void setDeletion(TaskDeletionEnum deletion) {
    this.deletion = deletion;
  }
}
