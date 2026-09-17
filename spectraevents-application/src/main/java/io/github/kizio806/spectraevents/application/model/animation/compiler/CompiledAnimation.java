package io.github.kizio806.spectraevents.application.model.animation.compiler;

import io.github.kizio806.spectraevents.core.visual.animation.AnimationDefinition;
import io.github.kizio806.spectraevents.core.visual.animation.AnimationTime;
import io.github.kizio806.spectraevents.core.visual.animation.ModelPose;
import io.github.kizio806.spectraevents.core.visual.model.ModelDefinition;
import io.github.kizio806.spectraevents.core.visual.model.ModelPartDefinition;
import io.github.kizio806.spectraevents.core.visual.model.ModelPartId;
import io.github.kizio806.spectraevents.core.visual.model.ModelTransform;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeSet;

/** Immutable precompiled animation ready for execution and fast pose evaluation. */
public class CompiledAnimation {
  private final AnimationDefinition definition;
  private final List<CompiledTrack> tracks;
  private final List<AnimationTime> boundaryScheduleTimes;

  public CompiledAnimation(AnimationDefinition definition, List<CompiledTrack> tracks) {
    this.definition = Objects.requireNonNull(definition, "definition cannot be null");
    Objects.requireNonNull(tracks, "tracks cannot be null");
    this.tracks = List.copyOf(tracks);

    // Precompute distinct boundary schedule times across all precompiled tracks
    TreeSet<AnimationTime> boundarySet = new TreeSet<>();
    boundarySet.add(AnimationTime.ZERO);
    boundarySet.add(definition.duration().time());

    for (CompiledTrack track : tracks) {
      for (CompiledSegment seg : track.segments()) {
        boundarySet.add(seg.startTime());
        boundarySet.add(seg.endTime());
      }
    }
    for (var cue : definition.cues()) {
      boundarySet.add(cue.time());
    }
    this.boundaryScheduleTimes = List.copyOf(boundarySet);
  }

  public AnimationDefinition definition() {
    return definition;
  }

  public List<CompiledTrack> tracks() {
    return tracks;
  }

  public List<AnimationTime> boundaryScheduleTimes() {
    return boundaryScheduleTimes;
  }

  /**
   * Evaluates current model pose deterministically at any playhead timestamp. O(tracks * log
   * keyframes).
   */
  public ModelPose evaluatePose(AnimationTime playhead, ModelDefinition modelDef) {
    Objects.requireNonNull(playhead, "playhead cannot be null");
    Objects.requireNonNull(modelDef, "modelDef cannot be null");

    ModelTransform rootTransform = ModelTransform.IDENTITY;
    Map<ModelPartId, ModelTransform> partTransforms = new HashMap<>();

    // Base part local transforms
    for (ModelPartDefinition part : modelDef.parts()) {
      partTransforms.put(part.partId(), part.localTransform());
    }

    // Apply track updates
    for (CompiledTrack track : tracks) {
      if (track.target().isRoot()) {
        rootTransform = track.evaluate(playhead, rootTransform);
      } else {
        ModelPartId partId = track.target().partId();
        ModelTransform baseTransform = partTransforms.getOrDefault(partId, ModelTransform.IDENTITY);
        ModelTransform evaluated = track.evaluate(playhead, baseTransform);
        partTransforms.put(partId, evaluated);
      }
    }

    return ModelPose.of(rootTransform, partTransforms);
  }
}
