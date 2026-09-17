package io.github.kizio806.spectraevents.application.model.animation.spec;

import java.util.List;
import java.util.Map;

/** Authoring DTO for an entire animation definition in model YAML. */
public class AnimationSpec {
  private String duration;
  private String loop;
  private Integer loopCount;
  private Map<String, AnimationTrackSpec> tracks;
  private List<TimelineCueSpec> cues;
  private String recovery;

  public String getDuration() {
    return duration;
  }

  public void setDuration(String duration) {
    this.duration = duration;
  }

  public String getLoop() {
    return loop;
  }

  public void setLoop(String loop) {
    this.loop = loop;
  }

  public Integer getLoopCount() {
    return loopCount;
  }

  public void setLoopCount(Integer loopCount) {
    this.loopCount = loopCount;
  }

  public Map<String, AnimationTrackSpec> getTracks() {
    return tracks;
  }

  public void setTracks(Map<String, AnimationTrackSpec> tracks) {
    this.tracks = tracks;
  }

  public List<TimelineCueSpec> getCues() {
    return cues;
  }

  public void setCues(List<TimelineCueSpec> cues) {
    this.cues = cues;
  }

  public String getRecovery() {
    return recovery;
  }

  public void setRecovery(String recovery) {
    this.recovery = recovery;
  }
}
