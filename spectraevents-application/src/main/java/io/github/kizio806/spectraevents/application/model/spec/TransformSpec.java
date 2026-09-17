package io.github.kizio806.spectraevents.application.model.spec;

import java.util.List;

/** Authoring DTO spec for a part transform in YAML. */
public class TransformSpec {
  private List<Float> translation;
  private RotationSpec rotation;
  private List<Float> scale;
  private List<Float> pivot;

  public List<Float> getTranslation() {
    return translation;
  }

  public void setTranslation(List<Float> translation) {
    this.translation = translation;
  }

  public RotationSpec getRotation() {
    return rotation;
  }

  public void setRotation(RotationSpec rotation) {
    this.rotation = rotation;
  }

  public List<Float> getScale() {
    return scale;
  }

  public void setScale(List<Float> scale) {
    this.scale = scale;
  }

  public List<Float> getPivot() {
    return pivot;
  }

  public void setPivot(List<Float> pivot) {
    this.pivot = pivot;
  }

  public static class RotationSpec {
    private List<Float> euler;
    private List<Float> quaternion;

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
  }
}
