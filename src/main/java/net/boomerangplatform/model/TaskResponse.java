package net.boomerangplatform.model;

import java.util.ArrayList;
import java.util.List;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class TaskResponse extends Response {

  private List<TaskResponseResultParameter> results = new ArrayList<>();

  public TaskResponse() {
    // Do nothing
  }

  public TaskResponse(String code, String desc, List<TaskResponseResultParameter> results) {
    super(code, desc);
    this.results = results;
  }

  public List<TaskResponseResultParameter> getResults() {
    return results;
  }

  public void setResults(List<TaskResponseResultParameter> results) {
    this.results = results;
  }
}
