package io.github.kizio806.spectraevents.application.model.animation.compiler;

import io.github.kizio806.spectraevents.core.visual.animation.AnimationDefinition;
import io.github.kizio806.spectraevents.core.visual.animation.AnimationTime;
import io.github.kizio806.spectraevents.core.visual.animation.AnimationTrackType;
import io.github.kizio806.spectraevents.core.visual.animation.ModelPose;
import io.github.kizio806.spectraevents.core.visual.model.ModelDefinition;
import io.github.kizio806.spectraevents.core.visual.model.ModelPartDefinition;
import io.github.kizio806.spectraevents.core.visual.model.ModelPartId;
import io.github.kizio806.spectraevents.core.visual.model.ModelTransform;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
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
    Map<ModelPartId, ModelTransform> localTransforms = new HashMap<>();

    // Base part local transforms
    for (ModelPartDefinition part : modelDef.parts()) {
      localTransforms.put(part.partId(), part.localTransform());
    }

    // Apply track updates
    for (CompiledTrack track : tracks) {
      if (track.target().isRoot()) {
        rootTransform =
            mergeChannel(rootTransform, track.evaluate(playhead, rootTransform), track.trackType());
      } else {
        ModelPartId partId = track.target().partId();
        ModelTransform baseTransform =
            localTransforms.getOrDefault(partId, ModelTransform.IDENTITY);
        localTransforms.put(
            partId,
            mergeChannel(
                baseTransform, track.evaluate(playhead, baseTransform), track.trackType()));
      }
    }

    Map<ModelPartId, ModelPartDefinition> partsById = new HashMap<>();
    for (ModelPartDefinition part : modelDef.parts()) {
      partsById.put(part.partId(), part);
    }
    Map<ModelPartId, ModelTransform> composedTransforms = new HashMap<>();
    Set<ModelPartId> visiting = new HashSet<>();
    for (ModelPartDefinition part : modelDef.parts()) {
      composeTransform(
          part, partsById, localTransforms, rootTransform, composedTransforms, visiting);
    }

    return ModelPose.of(rootTransform, composedTransforms);
  }

  private static ModelTransform mergeChannel(
      ModelTransform base, ModelTransform evaluated, AnimationTrackType trackType) {
    return switch (trackType) {
      case TRANSLATION ->
          new ModelTransform(evaluated.translation(), base.rotation(), base.scale(), base.pivot());
      case ROTATION ->
          new ModelTransform(base.translation(), evaluated.rotation(), base.scale(), base.pivot());
      case SCALE ->
          new ModelTransform(base.translation(), base.rotation(), evaluated.scale(), base.pivot());
      case VISIBILITY -> base;
    };
  }

  private static ModelTransform composeTransform(
      ModelPartDefinition part,
      Map<ModelPartId, ModelPartDefinition> partsById,
      Map<ModelPartId, ModelTransform> localTransforms,
      ModelTransform rootTransform,
      Map<ModelPartId, ModelTransform> composedTransforms,
      Set<ModelPartId> visiting) {
    ModelTransform existing = composedTransforms.get(part.partId());
    if (existing != null) {
      return existing;
    }
    if (!visiting.add(part.partId())) {
      throw new IllegalArgumentException("Model hierarchy contains a cycle at " + part.partId());
    }
    ModelTransform parentTransform = rootTransform;
    if (part.parentPartId() != null) {
      ModelPartDefinition parent = partsById.get(part.parentPartId());
      if (parent == null) {
        throw new IllegalArgumentException(
            "Model hierarchy references missing parent " + part.parentPartId());
      }
      parentTransform =
          composeTransform(
              parent, partsById, localTransforms, rootTransform, composedTransforms, visiting);
    }
    ModelTransform local = localTransforms.getOrDefault(part.partId(), part.localTransform());
    ModelTransform composed = local.compose(parentTransform);
    visiting.remove(part.partId());
    composedTransforms.put(part.partId(), composed);
    return composed;
  }
}
