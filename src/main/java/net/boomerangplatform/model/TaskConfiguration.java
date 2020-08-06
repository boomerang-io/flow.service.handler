package net.boomerangplatform.model;

import org.springframework.beans.factory.annotation.Autowired;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import net.boomerangplatform.service.ConfigurationServiceImpl;


@JsonIgnoreProperties
public class TaskConfiguration {

  private Boolean debug;

  private TaskDeletion deletion;
  
  @Autowired
  private ConfigurationServiceImpl configurationService;

//  Defaults to false
	public boolean getDebug() {
		return debug ;
	}

	public void setDebug(Boolean debug) {
		this.debug = debug;
	}

	public TaskDeletion getDeletion() {
		return deletion != null ? deletion : configurationService.getTaskDeletion();
	}

	public void setDeletion(TaskDeletion deletion) {
		this.deletion = deletion;
	}
}
