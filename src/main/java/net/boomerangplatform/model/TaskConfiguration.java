package net.boomerangplatform.model;

import org.springframework.beans.factory.annotation.Value;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties
public class TaskConfiguration {
	
  @Value("${kube.worker.job.deletion}")
  protected TaskDeletion kubeWorkerJobDeletion;

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
		return deletion != null ? deletion : kubeWorkerJobDeletion;
	}

	public void setDeletion(TaskDeletion deletion) {
		this.deletion = deletion;
	}
}
