package io.github.kizio806.spectraevents.application.model.spec;

import io.github.kizio806.spectraevents.application.model.animation.spec.AnimationSpec;
import java.util.Map;

/** Authoring DTO spec for a 3D model definition YAML file. */
public class ModelSpec {
  private String id;
  private Map<String, ModelPartSpec> parts;
  private Map<String, InteractionSpec> interactions;
  private Map<String, AnimationSpec> animations;

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public Map<String, ModelPartSpec> getParts() {
    return parts;
  }

  public void setParts(Map<String, ModelPartSpec> parts) {
    this.parts = parts;
  }

  public Map<String, InteractionSpec> getInteractions() {
    return interactions;
  }

  public void setInteractions(Map<String, InteractionSpec> interactions) {
    this.interactions = interactions;
  }

  public Map<String, AnimationSpec> getAnimations() {
    return animations;
  }

  public void setAnimations(Map<String, AnimationSpec> animations) {
    this.animations = animations;
  }
}
