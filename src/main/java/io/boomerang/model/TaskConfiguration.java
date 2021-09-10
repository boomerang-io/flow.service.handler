package io.boomerang.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties
public class TaskConfiguration {

  private Boolean debug = false;

  private int timeout;

  private TaskDeletionEnum deletion;

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
