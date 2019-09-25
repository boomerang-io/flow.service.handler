
package net.boomerangplatform.model.argo;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({"annotations", "clusterName", "creationTimestamp", "generation", "labels",
    "name", "namespace", "resourceVersion", "selfLink", "uid"})
public class Metadata extends GeneralProperties {

  private Annotations annotations;
  private String clusterName;
  private String creationTimestamp;
  private Integer generation;
  private Labels labels;
  private String name;
  private String namespace;
  private String resourceVersion;
  private String selfLink;
  private String uid;

  public Metadata() {
    // Do nothing
  }

  public Annotations getAnnotations() {
    return annotations;
  }

  public void setAnnotations(Annotations annotations) {
    this.annotations = annotations;
  }

  public String getClusterName() {
    return clusterName;
  }

  public void setClusterName(String clusterName) {
    this.clusterName = clusterName;
  }

  public String getCreationTimestamp() {
    return creationTimestamp;
  }

  public void setCreationTimestamp(String creationTimestamp) {
    this.creationTimestamp = creationTimestamp;
  }

  public Integer getGeneration() {
    return generation;
  }

  public void setGeneration(Integer generation) {
    this.generation = generation;
  }

  public Labels getLabels() {
    return labels;
  }

  public void setLabels(Labels labels) {
    this.labels = labels;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getNamespace() {
    return namespace;
  }

  public void setNamespace(String namespace) {
    this.namespace = namespace;
  }

  public String getResourceVersion() {
    return resourceVersion;
  }

  public void setResourceVersion(String resourceVersion) {
    this.resourceVersion = resourceVersion;
  }

  public String getSelfLink() {
    return selfLink;
  }

  public void setSelfLink(String selfLink) {
    this.selfLink = selfLink;
  }

  public String getUid() {
    return uid;
  }

  public void setUid(String uid) {
    this.uid = uid;
  }

}
