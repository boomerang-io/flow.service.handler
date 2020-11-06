package net.boomerangplatform.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;


@JsonIgnoreProperties
public class TaskConfiguration {

  private Boolean debug;

  private TaskDeletionEnum deletion;

	public Boolean getDebug() {
		return debug ;
	}

	public void setDebug(Boolean debug) {
		this.debug = debug;
	}

	public TaskDeletionEnum getDeletion() {
		return deletion ;
	}

	public void setDeletion(TaskDeletionEnum deletion) {
		this.deletion = deletion;
	}
}
