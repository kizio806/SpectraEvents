package io.github.kizio806.spectraevents.application.model.spec;

import java.util.List;

/** Authoring DTO spec for an interaction hitbox in a 3D model definition YAML. */
public class InteractionSpec {
  private String parent;
  private List<Float> offset;
  private Float width;
  private Float height;
  private Boolean responsive;

  public String getParent() {
    return parent;
  }

  public void setParent(String parent) {
    this.parent = parent;
  }

  public List<Float> getOffset() {
    return offset;
  }

  public void setOffset(List<Float> offset) {
    this.offset = offset;
  }

  public Float getWidth() {
    return width;
  }

  public void setWidth(Float width) {
    this.width = width;
  }

  public Float getHeight() {
    return height;
  }

  public void setHeight(Float height) {
    this.height = height;
  }

  public Boolean getResponsive() {
    return responsive;
  }

  public void setResponsive(Boolean responsive) {
    this.responsive = responsive;
  }
}
