package io.github.kizio806.spectraevents.application.model.animation.spec;

import java.util.List;

/** Authoring DTO for tracks targeting a single model part or root in animation YAML. */
public class AnimationTrackSpec {
  private List<KeyframeSpec> translation;
  private List<KeyframeSpec> rotation;
  private List<KeyframeSpec> scale;

  public List<KeyframeSpec> getTranslation() {
    return translation;
  }

  public void setTranslation(List<KeyframeSpec> translation) {
    this.translation = translation;
  }

  public List<KeyframeSpec> getRotation() {
    return rotation;
  }

  public void setRotation(List<KeyframeSpec> rotation) {
    this.rotation = rotation;
  }

  public List<KeyframeSpec> getScale() {
    return scale;
  }

  public void setScale(List<KeyframeSpec> scale) {
    this.scale = scale;
  }
}
