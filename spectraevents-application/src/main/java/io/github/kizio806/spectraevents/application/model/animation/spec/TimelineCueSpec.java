package io.github.kizio806.spectraevents.application.model.animation.spec;

/** Authoring DTO for a timeline cue marker in animation YAML. */
public class TimelineCueSpec {
  private String at;
  private String id;

  public String getAt() {
    return at;
  }

  public void setAt(String at) {
    this.at = at;
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }
}
