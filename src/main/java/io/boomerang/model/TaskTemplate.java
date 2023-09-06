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
    //TaskRunEntity has an ID but on the TaskRequest this is the ref
    this.setTaskRunRef(entity.getId());
    //TaskRunEntity has layered elements under Spec, the request is flat.
    BeanUtils.copyProperties(entity.getSpec(), this);
  }
}
