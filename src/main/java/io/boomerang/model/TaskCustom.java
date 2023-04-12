package io.boomerang.model;

import org.springframework.beans.BeanUtils;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.boomerang.model.ref.TaskRunEntity;

@JsonIgnoreProperties
public class TaskCustom extends TaskRequest {
  
  public TaskCustom() {
    
  }
  
  public TaskCustom(TaskRunEntity entity) {
    BeanUtils.copyProperties(entity, this);
    BeanUtils.copyProperties(entity.getSpec(), this);
  }
}
