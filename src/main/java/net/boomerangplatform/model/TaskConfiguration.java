package net.boomerangplatform.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;


@JsonIgnoreProperties
public class TaskConfiguration {

  private Boolean debug;

  private Boolean lifecycle;

  private TaskDeletionEnum deletion;

  public Boolean getDebug() {
    return debug;
  }

  public void setDebug(Boolean debug) {
    this.debug = debug;
  }

  public Boolean getLifecycle() {
    return lifecycle;
  }

  public void setLifecycle(Boolean lifecycle) {
    this.lifecycle = lifecycle;
  }

  public TaskDeletionEnum getDeletion() {
    return deletion;
  }

  public void setDeletion(TaskDeletionEnum deletion) {
    this.deletion = deletion;
  }

}
