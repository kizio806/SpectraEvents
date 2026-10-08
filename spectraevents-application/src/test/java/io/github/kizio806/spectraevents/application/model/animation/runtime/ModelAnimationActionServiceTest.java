package io.github.kizio806.spectraevents.application.model.animation.runtime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.github.kizio806.spectraevents.application.execution.FatalActionException;
import io.github.kizio806.spectraevents.application.model.animation.compiler.AnimationCompiler;
import io.github.kizio806.spectraevents.application.model.animation.registry.AnimationDefinitionRegistry;
import io.github.kizio806.spectraevents.application.model.animation.spec.AnimationSpec;
import io.github.kizio806.spectraevents.application.model.animation.spec.AnimationTrackSpec;
import io.github.kizio806.spectraevents.application.model.animation.spec.KeyframeSpec;
import io.github.kizio806.spectraevents.application.model.registry.ModelDefinitionRegistry;
import io.github.kizio806.spectraevents.application.model.runtime.DiscoveredModelEntity;
import io.github.kizio806.spectraevents.application.model.runtime.ModelAnchor;
import io.github.kizio806.spectraevents.application.model.runtime.ModelRuntimeId;
import io.github.kizio806.spectraevents.application.model.runtime.ModelRuntimeService;
import io.github.kizio806.spectraevents.application.model.runtime.RenderedModelHandle;
import io.github.kizio806.spectraevents.application.model.runtime.RenderedPartHandle;
import io.github.kizio806.spectraevents.application.port.AnimationSchedulerPort;
import io.github.kizio806.spectraevents.application.port.ModelRendererPort;
import io.github.kizio806.spectraevents.core.event.runtime.EventInstanceId;
import io.github.kizio806.spectraevents.core.visual.animation.AnimationId;
import io.github.kizio806.spectraevents.core.visual.model.ItemAssetRef;
import io.github.kizio806.spectraevents.core.visual.model.ModelDefinition;
import io.github.kizio806.spectraevents.core.visual.model.ModelId;
import io.github.kizio806.spectraevents.core.visual.model.ModelPartDefinition;
import io.github.kizio806.spectraevents.core.visual.model.ModelPartId;
import io.github.kizio806.spectraevents.core.visual.model.ModelPartType;
import io.github.kizio806.spectraevents.core.visual.model.ModelRenderProperties;
import io.github.kizio806.spectraevents.core.visual.model.ModelTransform;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ModelAnimationActionServiceTest {
  private final EventInstanceId eventId = EventInstanceId.generate();
  private final ModelId modelId = new ModelId("test_model");
  private final AnimationId animationId = new AnimationId("hover");

  private AnimationDefinitionRegistry animationRegistry;
  private ActiveAnimationRegistry activeAnimationRegistry;
  private ModelRuntimeService modelRuntimeService;
  private ModelAnimationActionService service;

  @BeforeEach
  void setUp() {
    ModelDefinition modelDefinition = modelDefinition();
    ModelDefinitionRegistry modelRegistry = new ModelDefinitionRegistry();
    modelRegistry.register(modelDefinition);

    animationRegistry = new AnimationDefinitionRegistry();
    animationRegistry.register(modelId, compiledAnimation());
    activeAnimationRegistry = new ActiveAnimationRegistry();
    modelRuntimeService = new ModelRuntimeService(modelRegistry, new FakeModelRenderer());
    AnimationRuntimeService animationRuntimeService =
        new AnimationRuntimeService(
            animationRegistry,
            activeAnimationRegistry,
            new FakeModelRenderer(),
            new TestAnimationScheduler());
    service = new ModelAnimationActionService(modelRuntimeService, animationRuntimeService);
    modelRuntimeService.spawnModel(modelId, ModelAnchor.of("world", 0, 64, 0), eventId);
  }

  @Test
  void playsAnimationOnModelsOwnedByEvent() {
    int started =
        service.play(
            eventId,
            Map.of("model", "test_model", "animation", "hover", "loop", true, "speed", 1.5));

    assertEquals(1, started);
    assertEquals(1, activeAnimationRegistry.count());
  }

  @Test
  void rejectsActionWhenEventHasNoMatchingModel() {
    assertThrows(
        FatalActionException.class,
        () -> service.play(eventId, Map.of("model", "other", "animation", "hover")));
  }

  @Test
  void stopsAnimationsBeforeModelCleanup() {
    service.play(eventId, Map.of("animation", "hover"));

    service.stopForEvent(eventId);

    assertEquals(0, activeAnimationRegistry.count());
  }

  private ModelDefinition modelDefinition() {
    ModelPartDefinition part =
        new ModelPartDefinition(
            ModelPartId.of("body"),
            null,
            ModelPartType.ITEM_DISPLAY,
            ModelTransform.IDENTITY,
            ModelTransform.IDENTITY,
            ModelRenderProperties.DEFAULT,
            ItemAssetRef.of("minecraft:stone"));
    return new ModelDefinition(modelId, List.of(part), List.of());
  }

  private io.github.kizio806.spectraevents.application.model.animation.compiler.CompiledAnimation
      compiledAnimation() {
    AnimationSpec spec = new AnimationSpec();
    spec.setDuration("1s");
    KeyframeSpec start = new KeyframeSpec();
    start.setAt("0s");
    start.setValue(List.of(0.0f, 0.0f, 0.0f));
    KeyframeSpec end = new KeyframeSpec();
    end.setAt("1s");
    end.setValue(List.of(0.0f, 1.0f, 0.0f));
    AnimationTrackSpec track = new AnimationTrackSpec();
    track.setTranslation(List.of(start, end));
    spec.setTracks(Map.of("body", track));
    return new AnimationCompiler().compile(animationId, spec, java.util.Set.of("body"));
  }

  private static final class TestAnimationScheduler implements AnimationSchedulerPort {
    @Override
    public void schedule(Duration delay, Runnable task) {}
  }

  private static final class FakeModelRenderer implements ModelRendererPort {
    @Override
    public RenderedModelHandle spawnModel(
        ModelRuntimeId runtimeId,
        ModelDefinition definition,
        ModelAnchor anchor,
        EventInstanceId ownerEventId) {
      return new RenderedModelHandle(
          runtimeId,
          definition.id(),
          ownerEventId,
          anchor,
          Map.of(
              ModelPartId.of("body"),
              new RenderedPartHandle(ModelPartId.of("body"), UUID.randomUUID())),
          Map.of());
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
      return new ArrayList<>();
    }
  }
}
