package net.boomerangplatform.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties
public class TaskConfiguration {

  private Boolean debug;

  private TaskDeletion deletion;

//  Defaults to false
	public boolean getDebug() {
		return debug ;
	}

	public void setDebug(Boolean debug) {
		this.debug = debug;
	}

	public TaskDeletion getDeletion() {
		return deletion;
	}

	public void setDeletion(TaskDeletion deletion) {
		this.deletion = deletion;
	}
}
