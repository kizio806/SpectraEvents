package io.github.kizio806.spectraevents.application.model.animation.compiler;

import io.github.kizio806.spectraevents.application.config.validation.ValidationDiagnostic;
import io.github.kizio806.spectraevents.application.config.validation.ValidationDiagnostic.Severity;
import io.github.kizio806.spectraevents.application.model.animation.spec.AnimationSpec;
import io.github.kizio806.spectraevents.application.model.animation.spec.AnimationTrackSpec;
import io.github.kizio806.spectraevents.application.model.animation.spec.KeyframeSpec;
import io.github.kizio806.spectraevents.application.model.animation.spec.TimelineCueSpec;
import io.github.kizio806.spectraevents.core.visual.animation.AnimationDefinition;
import io.github.kizio806.spectraevents.core.visual.animation.AnimationDuration;
import io.github.kizio806.spectraevents.core.visual.animation.AnimationId;
import io.github.kizio806.spectraevents.core.visual.animation.AnimationTarget;
import io.github.kizio806.spectraevents.core.visual.animation.AnimationTime;
import io.github.kizio806.spectraevents.core.visual.animation.AnimationTrackType;
import io.github.kizio806.spectraevents.core.visual.animation.Easing;
import io.github.kizio806.spectraevents.core.visual.animation.LoopMode;
import io.github.kizio806.spectraevents.core.visual.animation.RecoveryPolicy;
import io.github.kizio806.spectraevents.core.visual.animation.RotationKeyframe;
import io.github.kizio806.spectraevents.core.visual.animation.RotationMode;
import io.github.kizio806.spectraevents.core.visual.animation.ScaleKeyframe;
import io.github.kizio806.spectraevents.core.visual.animation.TimelineCue;
import io.github.kizio806.spectraevents.core.visual.animation.Vector3Keyframe;
import io.github.kizio806.spectraevents.core.visual.model.EulerRotation;
import io.github.kizio806.spectraevents.core.visual.model.ModelTransform;
import io.github.kizio806.spectraevents.core.visual.model.Quaternion;
import io.github.kizio806.spectraevents.core.visual.model.Vector3;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Validates and compiles animation DTO specs into immutable, pre-calculated CompiledAnimation
 * instances.
 */
public class AnimationCompiler {

  public static final long MAX_ANIMATION_DURATION_NANOS = 3600L * 1_000_000_000L; // 1 hour
  public static final int MAX_TRACK_SEGMENTS_LIMIT = 1000;
  public static final int MAX_TOTAL_SEGMENTS_LIMIT = 10000;

  public CompiledAnimation compile(
      AnimationId animationId, AnimationSpec spec, Set<String> validPartIds) {
    Objects.requireNonNull(animationId, "animationId cannot be null");
    Objects.requireNonNull(spec, "spec cannot be null");

    List<ValidationDiagnostic> diagnostics = new ArrayList<>();

    // 1. Validate Duration
    if (spec.getDuration() == null || spec.getDuration().isBlank()) {
      diagnostics.add(
          new ValidationDiagnostic(
              Severity.ERROR,
              "ANIMATION_DURATION_BLANK",
              "duration",
              "Animation duration cannot be null or blank"));
    }

    AnimationTime durationTime = parseAnimationTime(spec.getDuration(), "duration", diagnostics);
    if (durationTime.nanoseconds() <= 0) {
      diagnostics.add(
          new ValidationDiagnostic(
              Severity.ERROR,
              "ANIMATION_DURATION_NOT_POSITIVE",
              "duration",
              "Animation duration must be positive"));
    }
    if (durationTime.nanoseconds() > MAX_ANIMATION_DURATION_NANOS) {
      diagnostics.add(
          new ValidationDiagnostic(
              Severity.ERROR,
              "ANIMATION_DURATION_EXCEEDS_MAX",
              "duration",
              "Animation duration exceeds maximum limit of 1 hour"));
    }

    // 2. Parse Loop Mode & Recovery Policy
    LoopMode loopMode = parseLoopMode(spec.getLoop(), "loop", diagnostics);
    int loopCount =
        spec.getLoopCount() != null ? spec.getLoopCount() : (loopMode == LoopMode.LOOP ? 0 : 1);
    RecoveryPolicy recoveryPolicy =
        parseRecoveryPolicy(spec.getRecovery(), "recovery", diagnostics);

    // 3. Compile Tracks
    Map<AnimationTarget, List<?>> rawTrackMap = new HashMap<>();
    List<CompiledTrack> compiledTracks = new ArrayList<>();

    if (spec.getTracks() != null) {
      for (Map.Entry<String, AnimationTrackSpec> entry : spec.getTracks().entrySet()) {
        String targetStr = entry.getKey();
        AnimationTrackSpec trackSpec = entry.getValue();

        if (targetStr == null || targetStr.isBlank()) {
          diagnostics.add(
              new ValidationDiagnostic(
                  Severity.ERROR, "TRACK_TARGET_BLANK", "tracks", "Track target cannot be blank"));
          continue;
        }

        AnimationTarget target;
        try {
          target = AnimationTarget.parse(targetStr);
        } catch (IllegalArgumentException e) {
          diagnostics.add(
              new ValidationDiagnostic(
                  Severity.ERROR, "INVALID_TRACK_TARGET", "tracks." + targetStr, e.getMessage()));
          continue;
        }

        if (!target.isRoot()
            && validPartIds != null
            && !validPartIds.contains(target.partId().value())) {
          diagnostics.add(
              new ValidationDiagnostic(
                  Severity.ERROR,
                  "UNKNOWN_TRACK_TARGET_PART",
                  "tracks." + targetStr,
                  "Target part '" + targetStr + "' does not exist in model"));
        }

        if (trackSpec == null) continue;

        // Translation Track
        if (trackSpec.getTranslation() != null && !trackSpec.getTranslation().isEmpty()) {
          List<Vector3Keyframe> kfs =
              compileVector3Keyframes(
                  trackSpec.getTranslation(),
                  durationTime,
                  "tracks." + targetStr + ".translation",
                  diagnostics);
          rawTrackMap.put(target, kfs);
          compiledTracks.add(compileTranslationTrack(target, kfs, durationTime));
        }

        // Rotation Track
        if (trackSpec.getRotation() != null && !trackSpec.getRotation().isEmpty()) {
          List<RotationKeyframe> kfs =
              compileRotationKeyframes(
                  trackSpec.getRotation(),
                  durationTime,
                  "tracks." + targetStr + ".rotation",
                  diagnostics);
          rawTrackMap.put(target, kfs);
          compiledTracks.add(compileRotationTrack(target, kfs, durationTime));
        }

        // Scale Track
        if (trackSpec.getScale() != null && !trackSpec.getScale().isEmpty()) {
          List<ScaleKeyframe> kfs =
              compileScaleKeyframes(
                  trackSpec.getScale(),
                  durationTime,
                  "tracks." + targetStr + ".scale",
                  diagnostics);
          rawTrackMap.put(target, kfs);
          compiledTracks.add(compileScaleTrack(target, kfs, durationTime));
        }
      }
    }

    // 4. Compile Cues
    List<TimelineCue> compiledCues = new ArrayList<>();
    if (spec.getCues() != null) {
      for (int i = 0; i < spec.getCues().size(); i++) {
        TimelineCueSpec cueSpec = spec.getCues().get(i);
        String cuePath = "cues[" + i + "]";

        if (cueSpec.getId() == null || cueSpec.getId().isBlank()) {
          diagnostics.add(
              new ValidationDiagnostic(
                  Severity.ERROR, "CUE_ID_BLANK", cuePath + ".id", "Cue ID cannot be blank"));
          continue;
        }

        AnimationTime cueTime = parseAnimationTime(cueSpec.getAt(), cuePath + ".at", diagnostics);
        if (cueTime.compareTo(durationTime) > 0) {
          diagnostics.add(
              new ValidationDiagnostic(
                  Severity.ERROR,
                  "CUE_TIME_EXCEEDS_DURATION",
                  cuePath + ".at",
                  "Cue time exceeds animation duration"));
        }

        try {
          compiledCues.add(new TimelineCue(cueTime, cueSpec.getId()));
        } catch (IllegalArgumentException e) {
          diagnostics.add(
              new ValidationDiagnostic(Severity.ERROR, "INVALID_CUE", cuePath, e.getMessage()));
        }
      }
      Collections.sort(compiledCues);
    }

    // Check total segment budget
    int totalSegments = 0;
    for (CompiledTrack track : compiledTracks) {
      if (track.segments().size() > MAX_TRACK_SEGMENTS_LIMIT) {
        diagnostics.add(
            new ValidationDiagnostic(
                Severity.ERROR,
                "TRACK_SEGMENTS_EXCEED_BUDGET",
                "tracks." + track.target(),
                "Track contains "
                    + track.segments().size()
                    + " segments, exceeding limit of "
                    + MAX_TRACK_SEGMENTS_LIMIT));
      }
      totalSegments += track.segments().size();
    }

    if (totalSegments > MAX_TOTAL_SEGMENTS_LIMIT) {
      diagnostics.add(
          new ValidationDiagnostic(
              Severity.ERROR,
              "ANIMATION_SEGMENTS_EXCEED_BUDGET",
              "tracks",
              "Animation total segments count is "
                  + totalSegments
                  + ", exceeding limit of "
                  + MAX_TOTAL_SEGMENTS_LIMIT));
    }

    // Check errors
    if (diagnostics.stream().anyMatch(d -> d.severity() == Severity.ERROR)) {
      throw new AnimationCompilerException(
          "Failed to compile animation '" + animationId.value() + "'", diagnostics);
    }

    AnimationDefinition definition =
        new AnimationDefinition(
            animationId,
            AnimationDuration.of(durationTime),
            loopMode,
            loopCount,
            rawTrackMap,
            compiledCues,
            recoveryPolicy);

    return new CompiledAnimation(definition, compiledTracks);
  }

  public AnimationTime parseAnimationTime(
      String text, String path, List<ValidationDiagnostic> diagnostics) {
    if (text == null || text.isBlank()) {
      return AnimationTime.ZERO;
    }
    String s = text.trim().toLowerCase(Locale.ROOT);
    try {
      if (s.endsWith("ms")) {
        long ms = Long.parseLong(s.substring(0, s.length() - 2));
        return AnimationTime.fromMillis(ms);
      } else if (s.endsWith("s")) {
        double sec = Double.parseDouble(s.substring(0, s.length() - 1));
        return AnimationTime.fromSeconds(sec);
      } else if (s.endsWith("t")) {
        long ticks = Long.parseLong(s.substring(0, s.length() - 1));
        return AnimationTime.fromTicks(ticks);
      } else {
        double sec = Double.parseDouble(s);
        return AnimationTime.fromSeconds(sec);
      }
    } catch (Exception e) {
      diagnostics.add(
          new ValidationDiagnostic(
              Severity.ERROR,
              "INVALID_TIME_SYNTAX",
              path,
              "Invalid time format '" + text + "'. Expected e.g. 5s, 250ms, 20t"));
      return AnimationTime.ZERO;
    }
  }

  private LoopMode parseLoopMode(
      String loopStr, String path, List<ValidationDiagnostic> diagnostics) {
    if (loopStr == null || loopStr.isBlank()) {
      return LoopMode.ONCE;
    }
    try {
      return LoopMode.valueOf(loopStr.toUpperCase(Locale.ROOT));
    } catch (IllegalArgumentException e) {
      diagnostics.add(
          new ValidationDiagnostic(
              Severity.ERROR,
              "UNKNOWN_LOOP_MODE",
              path,
              "Unknown loop mode '" + loopStr + "'. Expected ONCE, LOOP, PING_PONG"));
      return LoopMode.ONCE;
    }
  }

  private RecoveryPolicy parseRecoveryPolicy(
      String recStr, String path, List<ValidationDiagnostic> diagnostics) {
    if (recStr == null || recStr.isBlank()) {
      return RecoveryPolicy.RESUME;
    }
    try {
      return RecoveryPolicy.valueOf(recStr.toUpperCase(Locale.ROOT));
    } catch (IllegalArgumentException e) {
      diagnostics.add(
          new ValidationDiagnostic(
              Severity.ERROR,
              "UNKNOWN_RECOVERY_POLICY",
              path,
              "Unknown recovery policy '" + recStr + "'. Expected RESUME, RESTART, STOP"));
      return RecoveryPolicy.RESUME;
    }
  }

  private List<Vector3Keyframe> compileVector3Keyframes(
      List<KeyframeSpec> specs,
      AnimationTime duration,
      String path,
      List<ValidationDiagnostic> diagnostics) {
    List<Vector3Keyframe> kfs = new ArrayList<>();
    Set<AnimationTime> seenTimes = new HashSet<>();

    for (int i = 0; i < specs.size(); i++) {
      KeyframeSpec kSpec = specs.get(i);
      String kPath = path + "[" + i + "]";

      AnimationTime time = parseAnimationTime(kSpec.getAt(), kPath + ".at", diagnostics);
      if (time.compareTo(duration) > 0) {
        diagnostics.add(
            new ValidationDiagnostic(
                Severity.ERROR,
                "KEYFRAME_EXCEEDS_DURATION",
                kPath + ".at",
                "Keyframe time exceeds declared animation duration"));
      }

      if (!seenTimes.add(time)) {
        diagnostics.add(
            new ValidationDiagnostic(
                Severity.ERROR,
                "DUPLICATE_KEYFRAME_TIME",
                kPath + ".at",
                "Duplicate keyframe time: " + kSpec.getAt()));
      }

      Vector3 val = parseVector3(kSpec.getValue(), Vector3.ZERO, kPath + ".value", diagnostics);
      Easing easing = parseEasing(kSpec.getEasing(), kPath + ".easing", diagnostics);

      kfs.add(new Vector3Keyframe(time, val, easing));
    }

    Collections.sort(kfs);
    return kfs;
  }

  private List<RotationKeyframe> compileRotationKeyframes(
      List<KeyframeSpec> specs,
      AnimationTime duration,
      String path,
      List<ValidationDiagnostic> diagnostics) {
    List<RotationKeyframe> kfs = new ArrayList<>();
    Set<AnimationTime> seenTimes = new HashSet<>();

    for (int i = 0; i < specs.size(); i++) {
      KeyframeSpec kSpec = specs.get(i);
      String kPath = path + "[" + i + "]";

      AnimationTime time = parseAnimationTime(kSpec.getAt(), kPath + ".at", diagnostics);
      if (time.compareTo(duration) > 0) {
        diagnostics.add(
            new ValidationDiagnostic(
                Severity.ERROR,
                "KEYFRAME_EXCEEDS_DURATION",
                kPath + ".at",
                "Keyframe time exceeds declared animation duration"));
      }

      if (!seenTimes.add(time)) {
        diagnostics.add(
            new ValidationDiagnostic(
                Severity.ERROR,
                "DUPLICATE_KEYFRAME_TIME",
                kPath + ".at",
                "Duplicate keyframe time: " + kSpec.getAt()));
      }

      Easing easing = parseEasing(kSpec.getEasing(), kPath + ".easing", diagnostics);
      RotationMode rotMode =
          parseRotationMode(kSpec.getRotationMode(), kPath + ".rotation_mode", diagnostics);

      if (kSpec.getEuler() != null && !kSpec.getEuler().isEmpty()) {
        EulerRotation euler = parseEuler(kSpec.getEuler(), kPath + ".euler", diagnostics);
        kfs.add(RotationKeyframe.ofEuler(time, euler, easing, rotMode));
      } else if (kSpec.getQuaternion() != null && !kSpec.getQuaternion().isEmpty()) {
        Quaternion q = parseQuaternion(kSpec.getQuaternion(), kPath + ".quaternion", diagnostics);
        kfs.add(RotationKeyframe.of(time, q, easing, rotMode));
      } else {
        kfs.add(RotationKeyframe.of(time, Quaternion.IDENTITY, easing, rotMode));
      }
    }

    Collections.sort(kfs);
    return kfs;
  }

  private List<ScaleKeyframe> compileScaleKeyframes(
      List<KeyframeSpec> specs,
      AnimationTime duration,
      String path,
      List<ValidationDiagnostic> diagnostics) {
    List<ScaleKeyframe> kfs = new ArrayList<>();
    Set<AnimationTime> seenTimes = new HashSet<>();

    for (int i = 0; i < specs.size(); i++) {
      KeyframeSpec kSpec = specs.get(i);
      String kPath = path + "[" + i + "]";

      AnimationTime time = parseAnimationTime(kSpec.getAt(), kPath + ".at", diagnostics);
      if (time.compareTo(duration) > 0) {
        diagnostics.add(
            new ValidationDiagnostic(
                Severity.ERROR,
                "KEYFRAME_EXCEEDS_DURATION",
                kPath + ".at",
                "Keyframe time exceeds declared animation duration"));
      }

      if (!seenTimes.add(time)) {
        diagnostics.add(
            new ValidationDiagnostic(
                Severity.ERROR,
                "DUPLICATE_KEYFRAME_TIME",
                kPath + ".at",
                "Duplicate keyframe time: " + kSpec.getAt()));
      }

      Vector3 val = parseVector3(kSpec.getValue(), Vector3.ONE, kPath + ".value", diagnostics);
      Easing easing = parseEasing(kSpec.getEasing(), kPath + ".easing", diagnostics);

      kfs.add(new ScaleKeyframe(time, val, easing));
    }

    Collections.sort(kfs);
    return kfs;
  }

  private CompiledTrack compileTranslationTrack(
      AnimationTarget target, List<Vector3Keyframe> keyframes, AnimationTime totalDuration) {
    List<CompiledSegment> segments = new ArrayList<>();
    if (keyframes.isEmpty()) {
      return new CompiledTrack(target, AnimationTrackType.TRANSLATION, segments);
    }

    for (int i = 0; i < keyframes.size() - 1; i++) {
      Vector3Keyframe k1 = keyframes.get(i);
      Vector3Keyframe k2 = keyframes.get(i + 1);

      long nanos = k2.time().nanoseconds() - k1.time().nanoseconds();
      if (nanos <= 0) continue;

      if (k1.easing() == Easing.LINEAR) {
        int ticks = (int) Math.max(1L, nanos / 50_000_000L);
        ModelTransform t1 = ModelTransform.of(k1.value());
        ModelTransform t2 = ModelTransform.of(k2.value());
        segments.add(new CompiledSegment(k1.time(), k2.time(), t1, t2, ticks));
      } else {
        // Sample non-linear curve at 50ms sub-steps
        long stepNanos = 50_000_000L; // 1 tick
        int subSteps = (int) Math.max(1L, nanos / stepNanos);
        for (int s = 0; s < subSteps; s++) {
          double fractionStart = (double) s / (double) subSteps;
          double fractionEnd = (double) (s + 1) / (double) subSteps;

          double easedStart = k1.easing().evaluate(fractionStart);
          double easedEnd = k1.easing().evaluate(fractionEnd);

          Vector3 valStart = k1.value().lerp(k2.value(), (float) easedStart);
          Vector3 valEnd = k1.value().lerp(k2.value(), (float) easedEnd);

          AnimationTime tStart =
              AnimationTime.fromNanos(k1.time().nanoseconds() + (long) (fractionStart * nanos));
          AnimationTime tEnd =
              AnimationTime.fromNanos(k1.time().nanoseconds() + (long) (fractionEnd * nanos));

          segments.add(
              new CompiledSegment(
                  tStart, tEnd, ModelTransform.of(valStart), ModelTransform.of(valEnd), 1));
        }
      }
    }

    return new CompiledTrack(target, AnimationTrackType.TRANSLATION, segments);
  }

  private CompiledTrack compileRotationTrack(
      AnimationTarget target, List<RotationKeyframe> keyframes, AnimationTime totalDuration) {
    List<CompiledSegment> segments = new ArrayList<>();
    if (keyframes.isEmpty()) {
      return new CompiledTrack(target, AnimationTrackType.ROTATION, segments);
    }

    for (int i = 0; i < keyframes.size() - 1; i++) {
      RotationKeyframe k1 = keyframes.get(i);
      RotationKeyframe k2 = keyframes.get(i + 1);

      long nanos = k2.time().nanoseconds() - k1.time().nanoseconds();
      if (nanos <= 0) continue;

      boolean isContinuous =
          k1.rotationMode() == RotationMode.CONTINUOUS
              || k2.rotationMode() == RotationMode.CONTINUOUS;
      boolean isLinear = k1.easing() == Easing.LINEAR;

      if (isLinear && !isContinuous) {
        int ticks = (int) Math.max(1L, nanos / 50_000_000L);
        ModelTransform t1 = ModelTransform.of(Vector3.ZERO, k1.rotation());
        ModelTransform t2 = ModelTransform.of(Vector3.ZERO, k2.rotation());
        segments.add(new CompiledSegment(k1.time(), k2.time(), t1, t2, ticks));
      } else {
        // Sample non-linear or continuous rotation curve at 50ms sub-steps
        long stepNanos = 50_000_000L; // 1 tick
        int subSteps = (int) Math.max(1L, nanos / stepNanos);
        for (int s = 0; s < subSteps; s++) {
          double fractionStart = (double) s / (double) subSteps;
          double fractionEnd = (double) (s + 1) / (double) subSteps;

          double easedStart = k1.easing().evaluate(fractionStart);
          double easedEnd = k1.easing().evaluate(fractionEnd);

          Quaternion qStart;
          Quaternion qEnd;

          if (isContinuous && k1.euler() != null && k2.euler() != null) {
            EulerRotation eStart = k1.euler().lerp(k2.euler(), (float) easedStart);
            EulerRotation eEnd = k1.euler().lerp(k2.euler(), (float) easedEnd);
            qStart = eStart.toQuaternion();
            qEnd = eEnd.toQuaternion();
          } else {
            qStart = k1.rotation().slerp(k2.rotation(), (float) easedStart);
            qEnd = k1.rotation().slerp(k2.rotation(), (float) easedEnd);
          }

          AnimationTime tStart =
              AnimationTime.fromNanos(k1.time().nanoseconds() + (long) (fractionStart * nanos));
          AnimationTime tEnd =
              AnimationTime.fromNanos(k1.time().nanoseconds() + (long) (fractionEnd * nanos));

          segments.add(
              new CompiledSegment(
                  tStart,
                  tEnd,
                  ModelTransform.of(Vector3.ZERO, qStart),
                  ModelTransform.of(Vector3.ZERO, qEnd),
                  1));
        }
      }
    }

    return new CompiledTrack(target, AnimationTrackType.ROTATION, segments);
  }

  private CompiledTrack compileScaleTrack(
      AnimationTarget target, List<ScaleKeyframe> keyframes, AnimationTime totalDuration) {
    List<CompiledSegment> segments = new ArrayList<>();
    if (keyframes.isEmpty()) {
      return new CompiledTrack(target, AnimationTrackType.SCALE, segments);
    }

    for (int i = 0; i < keyframes.size() - 1; i++) {
      ScaleKeyframe k1 = keyframes.get(i);
      ScaleKeyframe k2 = keyframes.get(i + 1);

      long nanos = k2.time().nanoseconds() - k1.time().nanoseconds();
      if (nanos <= 0) continue;

      if (k1.easing() == Easing.LINEAR) {
        int ticks = (int) Math.max(1L, nanos / 50_000_000L);
        ModelTransform t1 = ModelTransform.of(Vector3.ZERO, Quaternion.IDENTITY, k1.scale());
        ModelTransform t2 = ModelTransform.of(Vector3.ZERO, Quaternion.IDENTITY, k2.scale());
        segments.add(new CompiledSegment(k1.time(), k2.time(), t1, t2, ticks));
      } else {
        int subSteps = (int) Math.max(1L, nanos / 50_000_000L);
        for (int s = 0; s < subSteps; s++) {
          double fractionStart = (double) s / (double) subSteps;
          double fractionEnd = (double) (s + 1) / (double) subSteps;

          double easedStart = k1.easing().evaluate(fractionStart);
          double easedEnd = k1.easing().evaluate(fractionEnd);

          Vector3 sStart = k1.scale().lerp(k2.scale(), (float) easedStart);
          Vector3 sEnd = k1.scale().lerp(k2.scale(), (float) easedEnd);

          AnimationTime tStart =
              AnimationTime.fromNanos(k1.time().nanoseconds() + (long) (fractionStart * nanos));
          AnimationTime tEnd =
              AnimationTime.fromNanos(k1.time().nanoseconds() + (long) (fractionEnd * nanos));

          segments.add(
              new CompiledSegment(
                  tStart,
                  tEnd,
                  ModelTransform.of(Vector3.ZERO, Quaternion.IDENTITY, sStart),
                  ModelTransform.of(Vector3.ZERO, Quaternion.IDENTITY, sEnd),
                  1));
        }
      }
    }

    return new CompiledTrack(target, AnimationTrackType.SCALE, segments);
  }

  private Easing parseEasing(
      String easingStr, String path, List<ValidationDiagnostic> diagnostics) {
    if (easingStr == null || easingStr.isBlank()) {
      return Easing.LINEAR;
    }
    try {
      return Easing.valueOf(easingStr.toUpperCase(Locale.ROOT));
    } catch (IllegalArgumentException e) {
      diagnostics.add(
          new ValidationDiagnostic(
              Severity.ERROR, "UNKNOWN_EASING", path, "Unknown easing function: " + easingStr));
      return Easing.LINEAR;
    }
  }

  private RotationMode parseRotationMode(
      String modeStr, String path, List<ValidationDiagnostic> diagnostics) {
    if (modeStr == null || modeStr.isBlank()) {
      return RotationMode.SHORTEST;
    }
    try {
      return RotationMode.valueOf(modeStr.toUpperCase(Locale.ROOT));
    } catch (IllegalArgumentException e) {
      diagnostics.add(
          new ValidationDiagnostic(
              Severity.ERROR,
              "UNKNOWN_ROTATION_MODE",
              path,
              "Unknown rotation mode '" + modeStr + "'. Expected SHORTEST, CONTINUOUS"));
      return RotationMode.SHORTEST;
    }
  }

  private Vector3 parseVector3(
      List<Float> list, Vector3 defaultValue, String path, List<ValidationDiagnostic> diagnostics) {
    if (list == null || list.isEmpty()) {
      return defaultValue;
    }
    if (list.size() != 3) {
      diagnostics.add(
          new ValidationDiagnostic(
              Severity.ERROR,
              "INVALID_VECTOR3",
              path,
              "Vector3 must contain 3 float elements [x, y, z]"));
      return defaultValue;
    }
    try {
      return Vector3.of(list.get(0), list.get(1), list.get(2));
    } catch (IllegalArgumentException e) {
      diagnostics.add(
          new ValidationDiagnostic(Severity.ERROR, "INVALID_VECTOR3_VALUES", path, e.getMessage()));
      return defaultValue;
    }
  }

  private EulerRotation parseEuler(
      List<Float> list, String path, List<ValidationDiagnostic> diagnostics) {
    if (list == null || list.size() != 3) {
      diagnostics.add(
          new ValidationDiagnostic(
              Severity.ERROR,
              "INVALID_EULER",
              path,
              "Euler rotation must contain 3 float elements [pitch, yaw, roll]"));
      return EulerRotation.ZERO;
    }
    try {
      return new EulerRotation(list.get(0), list.get(1), list.get(2));
    } catch (IllegalArgumentException e) {
      diagnostics.add(
          new ValidationDiagnostic(Severity.ERROR, "INVALID_EULER_VALUES", path, e.getMessage()));
      return EulerRotation.ZERO;
    }
  }

  private Quaternion parseQuaternion(
      List<Float> list, String path, List<ValidationDiagnostic> diagnostics) {
    if (list == null || list.size() != 4) {
      diagnostics.add(
          new ValidationDiagnostic(
              Severity.ERROR,
              "INVALID_QUATERNION",
              path,
              "Quaternion must contain 4 float elements [x, y, z, w]"));
      return Quaternion.IDENTITY;
    }
    try {
      return Quaternion.of(list.get(0), list.get(1), list.get(2), list.get(3));
    } catch (IllegalArgumentException e) {
      diagnostics.add(
          new ValidationDiagnostic(
              Severity.ERROR, "INVALID_QUATERNION_VALUES", path, e.getMessage()));
      return Quaternion.IDENTITY;
    }
  }
}
