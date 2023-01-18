package io.boomerang.model;

import org.springframework.beans.BeanUtils;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.boomerang.model.ref.TaskRunEntity;

@JsonIgnoreProperties
public class TaskTemplate extends TaskRequest {
  
  public TaskTemplate() {
    
  }
  
  public TaskTemplate(TaskRunEntity entity) {
    BeanUtils.copyProperties(entity, this);
    BeanUtils.copyProperties(entity.getSpec(), this);
  }
}
