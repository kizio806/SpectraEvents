package io.github.kizio806.spectraevents.application.model.animation.runtime;

import io.github.kizio806.spectraevents.application.model.runtime.RenderedModelHandle;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/** Thread-safe registry holding all active 3D model animation playbacks. */
public class ActiveAnimationRegistry {

  private final Map<AnimationPlaybackId, ActiveAnimation> playbacksById = new ConcurrentHashMap<>();

  public void register(ActiveAnimation animation) {
    Objects.requireNonNull(animation, "animation cannot be null");
    playbacksById.put(animation.playbackId(), animation);
  }

  public Optional<ActiveAnimation> findById(AnimationPlaybackId playbackId) {
    Objects.requireNonNull(playbackId, "playbackId cannot be null");
    return Optional.ofNullable(playbacksById.get(playbackId));
  }

  public List<ActiveAnimation> findByModelHandle(RenderedModelHandle modelHandle) {
    Objects.requireNonNull(modelHandle, "modelHandle cannot be null");
    List<ActiveAnimation> result = new ArrayList<>();
    for (ActiveAnimation anim : playbacksById.values()) {
      if (anim.modelHandle().equals(modelHandle)) {
        result.add(anim);
      }
    }
    return result;
  }

  public List<ActiveAnimation> getAll() {
    return List.copyOf(playbacksById.values());
  }

  public boolean remove(AnimationPlaybackId playbackId) {
    Objects.requireNonNull(playbackId, "playbackId cannot be null");
    return playbacksById.remove(playbackId) != null;
  }

  public void removeAllForModel(RenderedModelHandle modelHandle) {
    Objects.requireNonNull(modelHandle, "modelHandle cannot be null");
    playbacksById.values().removeIf(anim -> anim.modelHandle().equals(modelHandle));
  }

  public void clear() {
    playbacksById.clear();
  }

  public int count() {
    return playbacksById.size();
  }
}
