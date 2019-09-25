package net.boomerangplatform.model.argo;

public class BmrgFlow extends GeneralProperties {

  private String displayName;
  private String finishedAt;
  private String id;
  private String name;
  private String phase;
  private String startedAt;
  private String type;

  public BmrgFlow() {
    // Do nothing
  }

  public String getDisplayName() {
    return displayName;
  }

  public void setDisplayName(String displayName) {
    this.displayName = displayName;
  }

  public String getFinishedAt() {
    return finishedAt;
  }

  public void setFinishedAt(String finishedAt) {
    this.finishedAt = finishedAt;
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getPhase() {
    return phase;
  }

  public void setPhase(String phase) {
    this.phase = phase;
  }

  public String getStartedAt() {
    return startedAt;
  }

  public void setStartedAt(String startedAt) {
    this.startedAt = startedAt;
  }

  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }
}
