package net.boomerangplatform.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import net.boomerangplatform.model.TaskDeletion;

@Service
public class ConfigurationServiceImpl implements ConfigurationService {

	  @Value("${kube.worker.job.deletion}")
	  private TaskDeletion workerDeletion;
	  
	  public TaskDeletion getTaskDeletion() {
		  return workerDeletion;
	  }
}
