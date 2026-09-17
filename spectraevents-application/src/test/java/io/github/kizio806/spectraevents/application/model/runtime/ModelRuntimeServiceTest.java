package io.github.kizio806.spectraevents.application.model.runtime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.kizio806.spectraevents.application.model.registry.ModelDefinitionRegistry;
import io.github.kizio806.spectraevents.application.port.ModelRendererPort;
import io.github.kizio806.spectraevents.core.event.runtime.EventInstanceId;
import io.github.kizio806.spectraevents.core.visual.model.ItemAssetRef;
import io.github.kizio806.spectraevents.core.visual.model.ModelDefinition;
import io.github.kizio806.spectraevents.core.visual.model.ModelId;
import io.github.kizio806.spectraevents.core.visual.model.ModelPartDefinition;
import io.github.kizio806.spectraevents.core.visual.model.ModelPartId;
import io.github.kizio806.spectraevents.core.visual.model.ModelPartType;
import io.github.kizio806.spectraevents.core.visual.model.ModelRenderProperties;
import io.github.kizio806.spectraevents.core.visual.model.ModelTransform;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ModelRuntimeServiceTest {

  private ModelDefinitionRegistry registry;
  private FakeModelRendererPort fakeRenderer;
  private ModelRuntimeService service;
  private ModelDefinition sampleDefinition;

  @BeforeEach
  void setUp() {
    registry = new ModelDefinitionRegistry();
    fakeRenderer = new FakeModelRendererPort();
    service = new ModelRuntimeService(registry, fakeRenderer);

    ModelPartDefinition corePart =
        new ModelPartDefinition(
            ModelPartId.of("core"),
            null,
            ModelPartType.ITEM_DISPLAY,
            ModelTransform.IDENTITY,
            ModelTransform.IDENTITY,
            ModelRenderProperties.DEFAULT,
            ItemAssetRef.of("minecraft:stone"));

    sampleDefinition =
        new ModelDefinition(new ModelId("sample_model"), List.of(corePart), List.of());
    registry.register(sampleDefinition);
  }

  @Test
  void testSpawnAndRemoveModel() {
    ModelAnchor anchor = ModelAnchor.of("world", 10.0, 64.0, 10.0);
    EventInstanceId ownerEvent = EventInstanceId.generate();

    RenderedModelHandle handle = service.spawnModel(sampleDefinition.id(), anchor, ownerEvent);

    assertNotNull(handle);
    assertTrue(service.getHandle(handle.runtimeId()).isPresent());
    assertEquals(1, service.getActiveInstances().size());

    // Idempotent remove
    assertTrue(service.removeModel(handle.runtimeId()));
    assertFalse(service.removeModel(handle.runtimeId()));
    assertTrue(service.getHandle(handle.runtimeId()).isEmpty());
    assertEquals(0, service.getActiveInstances().size());
  }

  @Test
  void testAtomicSpawnRollbackOnFailure() {
    fakeRenderer.setShouldFailSpawn(true);
    ModelAnchor anchor = ModelAnchor.of("world", 10.0, 64.0, 10.0);

    RenderedModelHandle handle = service.spawnModel(sampleDefinition.id(), anchor, null);

    assertNull(handle);
    assertEquals(0, service.getActiveInstances().size());
    assertTrue(fakeRenderer.wasRollbackPerformed());
  }

  @Test
  void testSafeReloadBehavior() {
    ModelAnchor anchor = ModelAnchor.of("world", 10.0, 64.0, 10.0);
    RenderedModelHandle handle = service.spawnModel(sampleDefinition.id(), anchor, null);

    // Replace definition in registry with new version
    ModelPartDefinition newPart =
        new ModelPartDefinition(
            ModelPartId.of("core_v2"),
            null,
            ModelPartType.ITEM_DISPLAY,
            ModelTransform.IDENTITY,
            ModelTransform.IDENTITY,
            ModelRenderProperties.DEFAULT,
            ItemAssetRef.of("minecraft:diamond_block"));
    ModelDefinition newDef =
        new ModelDefinition(sampleDefinition.id(), List.of(newPart), List.of());
    registry.replace(newDef);

    // Active instance retains its original handle and definition ID
    Optional<RenderedModelHandle> activeHandle = service.getHandle(handle.runtimeId());
    assertTrue(activeHandle.isPresent());
    assertEquals(sampleDefinition.id(), activeHandle.get().definitionId());
  }

  private static class FakeModelRendererPort implements ModelRendererPort {
    private boolean shouldFailSpawn = false;
    private boolean rollbackPerformed = false;
    private final Map<ModelRuntimeId, RenderedModelHandle> rendered = new HashMap<>();

    public void setShouldFailSpawn(boolean shouldFailSpawn) {
      this.shouldFailSpawn = shouldFailSpawn;
    }

    public boolean wasRollbackPerformed() {
      return rollbackPerformed;
    }

    @Override
    public RenderedModelHandle spawnModel(
        ModelRuntimeId runtimeId,
        ModelDefinition definition,
        ModelAnchor anchor,
        EventInstanceId ownerEventId) {
      if (shouldFailSpawn) {
        rollbackPerformed = true;
        return null;
      }
      Map<ModelPartId, RenderedPartHandle> parts = new HashMap<>();
      for (ModelPartDefinition p : definition.parts()) {
        parts.put(p.partId(), new RenderedPartHandle(p.partId(), UUID.randomUUID()));
      }
      RenderedModelHandle handle =
          new RenderedModelHandle(
              runtimeId, definition.id(), ownerEventId, anchor, parts, Map.of());
      rendered.put(runtimeId, handle);
      return handle;
    }

    @Override
    public boolean updateModelTransform(RenderedModelHandle handle, ModelAnchor newAnchor) {
      return rendered.containsKey(handle.runtimeId());
    }

    @Override
    public boolean updatePartTransform(
        RenderedModelHandle handle, ModelPartId partId, ModelTransform newLocalTransform) {
      return rendered.containsKey(handle.runtimeId());
    }

    @Override
    public boolean removeModel(RenderedModelHandle handle) {
      return rendered.remove(handle.runtimeId()) != null;
    }

    @Override
    public void removeAll() {
      rendered.clear();
    }

    @Override
    public List<DiscoveredModelEntity> reconcileEntities() {
      return List.of();
    }
  }
}
