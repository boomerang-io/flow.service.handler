package net.boomerangplatform.service;

import net.boomerangplatform.model.TaskDeletionEnum;

public interface ConfigurationService {
	  public TaskDeletionEnum getTaskDeletion();
	  
	  public Boolean getTaskDebug();
}
