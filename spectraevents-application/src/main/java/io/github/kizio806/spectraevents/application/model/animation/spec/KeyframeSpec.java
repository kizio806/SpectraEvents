package io.github.kizio806.spectraevents.application.model.animation.spec;

import java.util.List;

/** Authoring DTO for a single keyframe in an animation track YAML. */
public class KeyframeSpec {
  private String at;
  private List<Float> value;
  private List<Float> euler;
  private List<Float> quaternion;
  private String easing;
  private String rotationMode;

  public String getAt() {
    return at;
  }

  public void setAt(String at) {
    this.at = at;
  }

  public List<Float> getValue() {
    return value;
  }

  public void setValue(List<Float> value) {
    this.value = value;
  }

  public List<Float> getEuler() {
    return euler;
  }

  public void setEuler(List<Float> euler) {
    this.euler = euler;
  }

  public List<Float> getQuaternion() {
    return quaternion;
  }

  public void setQuaternion(List<Float> quaternion) {
    this.quaternion = quaternion;
  }

  public String getEasing() {
    return easing;
  }

  public void setEasing(String easing) {
    this.easing = easing;
  }

  public String getRotationMode() {
    return rotationMode;
  }

  public void setRotationMode(String rotationMode) {
    this.rotationMode = rotationMode;
  }
}
