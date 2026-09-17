package io.github.kizio806.spectraevents.application.model.animation.runtime;

import io.github.kizio806.spectraevents.application.model.animation.compiler.CompiledAnimation;
import io.github.kizio806.spectraevents.application.model.animation.compiler.CompiledSegment;
import io.github.kizio806.spectraevents.application.model.animation.compiler.CompiledTrack;
import io.github.kizio806.spectraevents.application.model.animation.registry.AnimationDefinitionRegistry;
import io.github.kizio806.spectraevents.application.model.runtime.RenderedModelHandle;
import io.github.kizio806.spectraevents.application.port.AnimationSchedulerPort;
import io.github.kizio806.spectraevents.application.port.ModelRendererPort;
import io.github.kizio806.spectraevents.core.visual.animation.AnimationId;
import io.github.kizio806.spectraevents.core.visual.animation.AnimationTime;
import io.github.kizio806.spectraevents.core.visual.model.ModelTransform;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Service managing lifecycle, segment dispatch, client Display interpolation, and control of 3D
 * animations.
 */
public class AnimationRuntimeService {

  private final AnimationDefinitionRegistry definitionRegistry;
  private final ActiveAnimationRegistry activeRegistry;
  private final ModelRendererPort modelRenderer;
  private final AnimationSchedulerPort scheduler;

  public AnimationRuntimeService(
      AnimationDefinitionRegistry definitionRegistry,
      ActiveAnimationRegistry activeRegistry,
      ModelRendererPort modelRenderer,
      AnimationSchedulerPort scheduler) {
    this.definitionRegistry =
        Objects.requireNonNull(definitionRegistry, "definitionRegistry cannot be null");
    this.activeRegistry = Objects.requireNonNull(activeRegistry, "activeRegistry cannot be null");
    this.modelRenderer = Objects.requireNonNull(modelRenderer, "modelRenderer cannot be null");
    this.scheduler = Objects.requireNonNull(scheduler, "scheduler cannot be null");
  }

  public AnimationPlaybackId play(RenderedModelHandle handle, AnimationId animationId) {
    return play(handle, animationId, PlaybackOptions.DEFAULT);
  }

  public AnimationPlaybackId play(
      RenderedModelHandle handle, AnimationId animationId, PlaybackOptions options) {
    Objects.requireNonNull(handle, "handle cannot be null");
    Objects.requireNonNull(animationId, "animationId cannot be null");
    Objects.requireNonNull(options, "options cannot be null");

    CompiledAnimation compiledAnimation =
        definitionRegistry
            .find(handle.definitionId(), animationId)
            .orElseThrow(
                () ->
                    new IllegalArgumentException(
                        "Animation '"
                            + animationId.value()
                            + "' not registered for model '"
                            + handle.definitionId().value()
                            + "'"));

    return play(handle, compiledAnimation, options);
  }

  public AnimationPlaybackId play(
      RenderedModelHandle handle, CompiledAnimation compiledAnimation, PlaybackOptions options) {
    Objects.requireNonNull(handle, "handle cannot be null");
    Objects.requireNonNull(compiledAnimation, "compiledAnimation cannot be null");
    Objects.requireNonNull(options, "options cannot be null");

    AnimationPlaybackId playbackId = AnimationPlaybackId.random();
    ActiveAnimation activeAnim =
        new ActiveAnimation(
            playbackId,
            handle,
            compiledAnimation,
            options.speed(),
            options.loopMode(),
            options.maxLoops(),
            options.recoveryPolicy(),
            options.cueListener());

    activeRegistry.register(activeAnim);
    stepAnimation(activeAnim);
    return playbackId;
  }

  public boolean pause(AnimationPlaybackId playbackId) {
    Objects.requireNonNull(playbackId, "playbackId cannot be null");
    Optional<ActiveAnimation> animOpt = activeRegistry.findById(playbackId);
    if (animOpt.isPresent()) {
      ActiveAnimation anim = animOpt.get();
      synchronized (anim) {
        if (anim.state() == PlaybackState.PLAYING) {
          anim.setState(PlaybackState.PAUSED);
          return true;
        }
      }
    }
    return false;
  }

  public boolean resume(AnimationPlaybackId playbackId) {
    Objects.requireNonNull(playbackId, "playbackId cannot be null");
    Optional<ActiveAnimation> animOpt = activeRegistry.findById(playbackId);
    if (animOpt.isPresent()) {
      ActiveAnimation anim = animOpt.get();
      synchronized (anim) {
        if (anim.state() == PlaybackState.PAUSED) {
          anim.setState(PlaybackState.PLAYING);
          stepAnimation(anim);
          return true;
        }
      }
    }
    return false;
  }

  public boolean stop(AnimationPlaybackId playbackId) {
    Objects.requireNonNull(playbackId, "playbackId cannot be null");
    Optional<ActiveAnimation> animOpt = activeRegistry.findById(playbackId);
    if (animOpt.isPresent()) {
      ActiveAnimation anim = animOpt.get();
      synchronized (anim) {
        anim.setState(PlaybackState.STOPPED);
      }
      activeRegistry.remove(playbackId);
      return true;
    }
    return false;
  }

  public boolean seek(AnimationPlaybackId playbackId, AnimationTime targetTime) {
    Objects.requireNonNull(playbackId, "playbackId cannot be null");
    Objects.requireNonNull(targetTime, "targetTime cannot be null");
    Optional<ActiveAnimation> animOpt = activeRegistry.findById(playbackId);
    if (animOpt.isPresent()) {
      ActiveAnimation anim = animOpt.get();
      synchronized (anim) {
        anim.setCurrentTime(targetTime);
        // Instant update without interpolation duration
        for (CompiledTrack track : anim.compiledAnimation().tracks()) {
          if (!track.target().isRoot() && track.target().partId() != null) {
            ModelTransform pose = track.evaluate(targetTime, ModelTransform.IDENTITY);
            if (pose != null) {
              modelRenderer.updatePartTransform(
                  anim.modelHandle(), track.target().partId(), pose, 0);
            }
          }
        }
      }
      return true;
    }
    return false;
  }

  public void stopAll(RenderedModelHandle handle) {
    Objects.requireNonNull(handle, "handle cannot be null");
    List<ActiveAnimation> activeAnims = activeRegistry.findByModelHandle(handle);
    for (ActiveAnimation anim : activeAnims) {
      synchronized (anim) {
        anim.setState(PlaybackState.STOPPED);
      }
      activeRegistry.remove(anim.playbackId());
    }
  }

  public Optional<AnimationPlaybackState> getPlaybackState(AnimationPlaybackId playbackId) {
    Objects.requireNonNull(playbackId, "playbackId cannot be null");
    return activeRegistry.findById(playbackId).map(ActiveAnimation::toSnapshotState);
  }

  public List<AnimationPlaybackState> getPlaybackStatesForModel(RenderedModelHandle handle) {
    Objects.requireNonNull(handle, "handle cannot be null");
    List<ActiveAnimation> list = activeRegistry.findByModelHandle(handle);
    List<AnimationPlaybackState> result = new ArrayList<>();
    for (ActiveAnimation anim : list) {
      result.add(anim.toSnapshotState());
    }
    return result;
  }

  private void stepAnimation(ActiveAnimation activeAnim) {
    synchronized (activeAnim) {
      if (activeAnim.state() != PlaybackState.PLAYING) {
        return;
      }

      int minStepTicks = Integer.MAX_VALUE;

      for (CompiledTrack track : activeAnim.compiledAnimation().tracks()) {
        CompiledSegment segment = activeAnim.getActiveSegment(track);
        if (segment != null) {
          int durationTicks = segment.interpolationDurationTicks();
          minStepTicks = Math.min(minStepTicks, Math.max(1, durationTicks));

          if (!track.target().isRoot() && track.target().partId() != null) {
            modelRenderer.updatePartTransform(
                activeAnim.modelHandle(),
                track.target().partId(),
                segment.targetTransform(),
                durationTicks);
          }
        }
      }

      if (minStepTicks == Integer.MAX_VALUE) {
        minStepTicks = 1;
      }

      final long stepTicks = minStepTicks;
      Duration stepDuration = Duration.ofMillis(stepTicks * 50);

      scheduler.schedule(
          stepDuration,
          () -> {
            synchronized (activeAnim) {
              if (activeAnim.state() == PlaybackState.PLAYING) {
                boolean completed = activeAnim.advanceTime(stepTicks * 50_000_000L);
                for (CompiledTrack track : activeAnim.compiledAnimation().tracks()) {
                  activeAnim.advanceTrackSegment(track);
                }
                if (completed) {
                  activeRegistry.remove(activeAnim.playbackId());
                } else {
                  stepAnimation(activeAnim);
                }
              }
            }
          });
    }
  }
}
