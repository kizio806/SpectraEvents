package io.github.kizio806.spectraevents.application.port;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.kizio806.spectraevents.application.model.runtime.DiscoveredModelEntity;
import io.github.kizio806.spectraevents.application.model.runtime.ModelAnchor;
import io.github.kizio806.spectraevents.application.model.runtime.ModelRuntimeId;
import io.github.kizio806.spectraevents.application.model.runtime.RenderedModelHandle;
import io.github.kizio806.spectraevents.core.event.runtime.EventInstanceId;
import io.github.kizio806.spectraevents.core.visual.model.ModelDefinition;
import io.github.kizio806.spectraevents.core.visual.model.ModelPartId;
import io.github.kizio806.spectraevents.core.visual.model.ModelTransform;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;

class ModelRendererPortTest {

  @Test
  void testDefaultUpdateModelTransformValidationAndDelegation() {
    AtomicBoolean called = new AtomicBoolean(false);
    ModelRendererPort port =
        new ModelRendererPort() {
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
            called.set(true);
            return true;
          }

          @Override
          public boolean updatePartTransform(
              RenderedModelHandle handle, ModelPartId partId, ModelTransform newLocalTransform) {
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
        };

    IllegalArgumentException exception =
        assertThrows(
            IllegalArgumentException.class, () -> port.updateModelTransform(null, null, -1));
    assertEquals("interpolationDurationTicks must be non-negative", exception.getMessage());

    assertTrue(port.updateModelTransform(null, null, 10));
    assertTrue(called.get());

    assertTrue(port.updatePartTransform(null, null, null, 5));
  }
}
