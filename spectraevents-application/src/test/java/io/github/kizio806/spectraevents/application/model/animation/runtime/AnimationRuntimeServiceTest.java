package io.github.kizio806.spectraevents.application.model.animation.runtime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.kizio806.spectraevents.application.model.animation.compiler.AnimationCompiler;
import io.github.kizio806.spectraevents.application.model.animation.compiler.CompiledAnimation;
import io.github.kizio806.spectraevents.application.model.animation.registry.AnimationDefinitionRegistry;
import io.github.kizio806.spectraevents.application.model.animation.spec.AnimationSpec;
import io.github.kizio806.spectraevents.application.model.animation.spec.AnimationTrackSpec;
import io.github.kizio806.spectraevents.application.model.animation.spec.KeyframeSpec;
import io.github.kizio806.spectraevents.application.model.animation.spec.TimelineCueSpec;
import io.github.kizio806.spectraevents.application.model.runtime.DiscoveredModelEntity;
import io.github.kizio806.spectraevents.application.model.runtime.ModelAnchor;
import io.github.kizio806.spectraevents.application.model.runtime.ModelRuntimeId;
import io.github.kizio806.spectraevents.application.model.runtime.RenderedModelHandle;
import io.github.kizio806.spectraevents.application.model.runtime.RenderedPartHandle;
import io.github.kizio806.spectraevents.application.port.AnimationSchedulerPort;
import io.github.kizio806.spectraevents.application.port.ModelRendererPort;
import io.github.kizio806.spectraevents.core.event.runtime.EventInstanceId;
import io.github.kizio806.spectraevents.core.visual.animation.AnimationId;
import io.github.kizio806.spectraevents.core.visual.animation.AnimationTime;
import io.github.kizio806.spectraevents.core.visual.model.ModelDefinition;
import io.github.kizio806.spectraevents.core.visual.model.ModelId;
import io.github.kizio806.spectraevents.core.visual.model.ModelPartId;
import io.github.kizio806.spectraevents.core.visual.model.ModelTransform;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AnimationRuntimeServiceTest {

  private AnimationDefinitionRegistry definitionRegistry;
  private ActiveAnimationRegistry activeRegistry;
  private MockModelRenderer modelRenderer;
  private TestScheduler scheduler;
  private AnimationRuntimeService service;

  private RenderedModelHandle mockHandle;
  private CompiledAnimation compiledAnim;
  private AnimationId animId;

  @BeforeEach
  void setUp() {
    definitionRegistry = new AnimationDefinitionRegistry();
    activeRegistry = new ActiveAnimationRegistry();
    modelRenderer = new MockModelRenderer();
    scheduler = new TestScheduler();

    service =
        new AnimationRuntimeService(definitionRegistry, activeRegistry, modelRenderer, scheduler);

    ModelId modelId = new ModelId("test_model");
    ModelRuntimeId runtimeId = ModelRuntimeId.of("run_1");
    ModelAnchor anchor = new ModelAnchor("world", 0, 100, 0, 0, 0);

    mockHandle =
        new RenderedModelHandle(
            runtimeId,
            modelId,
            null,
            anchor,
            Map.of(
                ModelPartId.of("part1"),
                new RenderedPartHandle(ModelPartId.of("part1"), UUID.randomUUID())),
            Map.of());

    // Compile sample animation
    AnimationCompiler compiler = new AnimationCompiler();
    animId = new AnimationId("wave");
    AnimationSpec spec = new AnimationSpec();
    spec.setDuration("1s");

    KeyframeSpec kf1 = new KeyframeSpec();
    kf1.setAt("0s");
    kf1.setValue(List.of(0.0f, 0.0f, 0.0f));

    KeyframeSpec kf2 = new KeyframeSpec();
    kf2.setAt("1s");
    kf2.setValue(List.of(0.0f, 5.0f, 0.0f));

    AnimationTrackSpec trackSpec = new AnimationTrackSpec();
    trackSpec.setTranslation(List.of(kf1, kf2));

    spec.setTracks(Map.of("part1", trackSpec));

    TimelineCueSpec cue = new TimelineCueSpec();
    cue.setAt("500ms");
    cue.setId("halfway");
    spec.setCues(List.of(cue));

    compiledAnim = compiler.compile(animId, spec, Set.of("part1"));
    definitionRegistry.register(modelId, compiledAnim);
  }

  @Test
  void testPlayAnimation() {
    AnimationPlaybackId playbackId = service.play(mockHandle, animId);
    assertNotNull(playbackId);

    Optional<AnimationPlaybackState> stateOpt = service.getPlaybackState(playbackId);
    assertTrue(stateOpt.isPresent());
    assertEquals(PlaybackState.PLAYING, stateOpt.get().state());
    assertEquals(1, activeRegistry.count());

    // Initial segment dispatch updated renderer
    assertEquals(1, modelRenderer.updateCalls.size());
    assertEquals(20, modelRenderer.updateCalls.get(0).interpolationDurationTicks());
  }

  @Test
  void testPauseAndResume() {
    AnimationPlaybackId playbackId = service.play(mockHandle, animId);

    assertTrue(service.pause(playbackId));
    assertEquals(PlaybackState.PAUSED, service.getPlaybackState(playbackId).get().state());

    assertTrue(service.resume(playbackId));
    assertEquals(PlaybackState.PLAYING, service.getPlaybackState(playbackId).get().state());
  }

  @Test
  void testStopAnimation() {
    AnimationPlaybackId playbackId = service.play(mockHandle, animId);

    assertTrue(service.stop(playbackId));
    assertFalse(service.getPlaybackState(playbackId).isPresent());
    assertEquals(0, activeRegistry.count());
  }

  @Test
  void testSeekAnimation() {
    AnimationPlaybackId playbackId = service.play(mockHandle, animId);

    assertTrue(service.seek(playbackId, AnimationTime.fromMillis(500)));
    assertEquals(
        AnimationTime.fromMillis(500), service.getPlaybackState(playbackId).get().currentTime());
  }

  @Test
  void testTimelineCueCallbackTriggered() {
    AtomicBoolean cueFired = new AtomicBoolean(false);

    PlaybackOptions options =
        PlaybackOptions.withCueListener(
            cue -> {
              if ("halfway".equals(cue.cueId())) {
                cueFired.set(true);
              }
            });

    AnimationPlaybackId playbackId = service.play(mockHandle, compiledAnim, options);
    assertNotNull(playbackId);

    // Run scheduled task to advance clock to 1s
    scheduler.runPending();

    assertTrue(cueFired.get());
  }

  @Test
  void testDeterministicConflictPolicyAndStopAll() {
    AnimationPlaybackId id1 = service.play(mockHandle, animId);
    assertEquals(1, activeRegistry.count());

    // Playing same animation again stops id1
    AnimationPlaybackId id2 = service.play(mockHandle, animId);
    assertFalse(service.getPlaybackState(id1).isPresent());
    assertTrue(service.getPlaybackState(id2).isPresent());
    assertEquals(1, activeRegistry.count());

    assertEquals(1, service.getPlaybackStatesForModel(mockHandle).size());
    assertEquals(1, service.getActivePlaybacks().size());

    service.stopAll(mockHandle);
    assertEquals(0, activeRegistry.count());
    assertTrue(service.getActivePlaybacks().isEmpty());
  }

  @Test
  void testRestorePlayback() {
    AnimationPlaybackState snapshot =
        new AnimationPlaybackState(
            AnimationPlaybackId.random(),
            mockHandle,
            animId,
            PlaybackState.PLAYING,
            AnimationTime.fromMillis(250),
            1.0f,
            io.github.kizio806.spectraevents.core.visual.animation.LoopMode.ONCE,
            0,
            io.github.kizio806.spectraevents.core.visual.animation.RecoveryPolicy.STOP);

    AnimationPlaybackId restoredId = service.restorePlayback(snapshot);
    assertNotNull(restoredId);
    assertEquals(1, activeRegistry.count());
    assertEquals(
        AnimationTime.fromMillis(250), service.getPlaybackState(restoredId).get().currentTime());
  }

  @Test
  void testComposedAnimationLifecycle() {
    io.github.kizio806.spectraevents.application.model.registry.ModelDefinitionRegistry modelReg =
        new io.github.kizio806.spectraevents.application.model.registry.ModelDefinitionRegistry();

    io.github.kizio806.spectraevents.core.visual.model.ModelPartDefinition part =
        new io.github.kizio806.spectraevents.core.visual.model.ModelPartDefinition(
            ModelPartId.of("part1"),
            null,
            io.github.kizio806.spectraevents.core.visual.model.ModelPartType.ITEM_DISPLAY,
            ModelTransform.IDENTITY,
            ModelTransform.IDENTITY,
            io.github.kizio806.spectraevents.core.visual.model.ModelRenderProperties.DEFAULT,
            new io.github.kizio806.spectraevents.core.visual.model.ItemAssetRef(
                "minecraft:diamond_helmet",
                io.github.kizio806.spectraevents.core.visual.model.DisplayTransformMode.HEAD));

    ModelDefinition modelDef =
        new ModelDefinition(new ModelId("test_model"), List.of(part), List.of(), Map.of());
    modelReg.register(modelDef);

    AnimationRuntimeService composedService =
        new AnimationRuntimeService(
            definitionRegistry, modelReg, activeRegistry, modelRenderer, scheduler);

    definitionRegistry.register(new ModelId("test_model"), compiledAnim);

    AnimationPlaybackId playbackId = composedService.play(mockHandle, animId);
    assertNotNull(playbackId);
    assertFalse(modelRenderer.updateCalls.isEmpty());

    // Run scheduler pending task
    scheduler.runPending();

    // Pause, Resume, Stop
    assertTrue(composedService.pause(playbackId));
    assertFalse(composedService.pause(playbackId)); // Already paused

    assertTrue(composedService.resume(playbackId));
    assertFalse(composedService.resume(playbackId)); // Already playing

    assertTrue(composedService.seek(playbackId, AnimationTime.fromMillis(500)));
    assertTrue(composedService.stop(playbackId));
    assertFalse(composedService.stop(playbackId)); // Already stopped
  }

  @Test
  void testQueriesAndBulkOperations() {
    definitionRegistry.register(new ModelId("test_model"), compiledAnim);
    AnimationPlaybackId id = service.play(mockHandle, animId);

    assertTrue(service.getPlaybackState(id).isPresent());
    assertFalse(service.getPlaybackState(AnimationPlaybackId.random()).isPresent());

    assertEquals(1, service.getActivePlaybacks().size());
    assertEquals(1, service.getPlaybackStatesForModel(mockHandle).size());

    RenderedModelHandle otherHandle =
        new RenderedModelHandle(
            ModelRuntimeId.of("run_other"),
            new ModelId("other_model"),
            null,
            new ModelAnchor("world", 0, 0, 0, 0, 0),
            Map.of(),
            Map.of());
    assertTrue(service.getPlaybackStatesForModel(otherHandle).isEmpty());

    assertFalse(service.pause(AnimationPlaybackId.random()));
    assertFalse(service.resume(AnimationPlaybackId.random()));
    assertFalse(service.stop(AnimationPlaybackId.random()));
    assertFalse(service.seek(AnimationPlaybackId.random(), AnimationTime.ZERO));

    service.stopAll(mockHandle);
    assertTrue(service.getActivePlaybacks().isEmpty());
  }

  // --- Test Helpers ---

  private static class TestScheduler implements AnimationSchedulerPort {
    private final List<Runnable> pendingTasks = new ArrayList<>();

    @Override
    public void schedule(Duration delay, Runnable task) {
      pendingTasks.add(task);
    }

    void runPending() {
      List<Runnable> copy = new ArrayList<>(pendingTasks);
      pendingTasks.clear();
      for (Runnable task : copy) {
        task.run();
      }
    }
  }

  private static class UpdateCall {
    private final int interpolationDurationTicks;

    UpdateCall(int interpolationDurationTicks) {
      this.interpolationDurationTicks = interpolationDurationTicks;
    }

    int interpolationDurationTicks() {
      return interpolationDurationTicks;
    }
  }

  private static class MockModelRenderer implements ModelRendererPort {
    private final List<UpdateCall> updateCalls = new ArrayList<>();

    @Override
    public RenderedModelHandle spawnModel(
        ModelRuntimeId runtimeId,
        ModelDefinition definition,
        ModelAnchor anchor,
        EventInstanceId ownerEventId) {
      return null;
    }

    @Override
    public boolean updateModelTransform(RenderedModelHandle handle, ModelAnchor newAnchor) {
      return true;
    }

    @Override
    public boolean updatePartTransform(
        RenderedModelHandle handle, ModelPartId partId, ModelTransform newLocalTransform) {
      return true;
    }

    @Override
    public boolean updatePartTransform(
        RenderedModelHandle handle,
        ModelPartId partId,
        ModelTransform newLocalTransform,
        int interpolationDurationTicks) {
      updateCalls.add(new UpdateCall(interpolationDurationTicks));
      return true;
    }

    @Override
    public boolean removeModel(RenderedModelHandle handle) {
      return true;
    }

    @Override
    public void removeAll() {}

    @Override
    public List<DiscoveredModelEntity> reconcileEntities() {
      return List.of();
    }
  }
}
