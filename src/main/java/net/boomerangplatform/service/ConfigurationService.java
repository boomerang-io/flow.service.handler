package net.boomerangplatform.service;

import net.boomerangplatform.model.TaskDeletion;

public interface ConfigurationService {
	  public TaskDeletion getTaskDeletion();
	  
	  public Boolean getTaskDebug();
}
