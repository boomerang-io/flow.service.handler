package net.boomerangplatform.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import net.boomerangplatform.model.TaskDeletionEnum;

@Service
public class ConfigurationServiceImpl implements ConfigurationService {

	  @Value("${kube.worker.job.deletion}")
	  private TaskDeletionEnum workerDeletion;

	  @Value("${kube.worker.debug}")
	  private Boolean workerDebug;
	  
	  public TaskDeletionEnum getTaskDeletion() {
		  return workerDeletion;
	  }
	  
	  public Boolean getTaskDebug() {
		  return workerDebug;
	  }
}
