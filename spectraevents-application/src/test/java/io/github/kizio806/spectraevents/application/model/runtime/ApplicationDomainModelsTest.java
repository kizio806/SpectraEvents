package io.github.kizio806.spectraevents.application.model.runtime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.github.kizio806.spectraevents.application.integration.IntegrationInitializationContext;
import io.github.kizio806.spectraevents.application.port.PlatformCapability;
import io.github.kizio806.spectraevents.application.port.PlatformDescriptor;
import io.github.kizio806.spectraevents.core.event.runtime.EventInstanceId;
import io.github.kizio806.spectraevents.core.visual.model.ModelId;
import java.util.UUID;
import java.util.logging.Logger;
import org.junit.jupiter.api.Test;

class ApplicationDomainModelsTest {

  @Test
  void testDiscoveredModelEntityAndResourceRole() {
    UUID uuid = UUID.randomUUID();
    ModelRuntimeId runtimeId = ModelRuntimeId.of("run_01");
    ModelId modelId = new ModelId("model_01");
    EventInstanceId eventId = EventInstanceId.generate();

    DiscoveredModelEntity entity =
        new DiscoveredModelEntity(
            uuid, runtimeId, modelId, "part_head", ResourceRole.VISUAL_PART, eventId);

    assertEquals(uuid, entity.entityUuid());
    assertEquals(runtimeId, entity.runtimeId());
    assertEquals(modelId, entity.definitionId());
    assertEquals("part_head", entity.partOrInteractionId());
    assertEquals(ResourceRole.VISUAL_PART, entity.role());
    assertEquals(eventId, entity.ownerEventId());

    assertEquals(ResourceRole.VISUAL_PART, ResourceRole.valueOf("VISUAL_PART"));
    assertEquals(ResourceRole.INTERACTION_HITBOX, ResourceRole.valueOf("INTERACTION_HITBOX"));

    assertThrows(
        NullPointerException.class,
        () ->
            new DiscoveredModelEntity(
                null, runtimeId, modelId, "part_head", ResourceRole.VISUAL_PART, eventId));
    assertThrows(
        NullPointerException.class,
        () ->
            new DiscoveredModelEntity(
                uuid, null, modelId, "part_head", ResourceRole.VISUAL_PART, eventId));
    assertThrows(
        NullPointerException.class,
        () ->
            new DiscoveredModelEntity(
                uuid, runtimeId, null, "part_head", ResourceRole.VISUAL_PART, eventId));
    assertThrows(
        NullPointerException.class,
        () ->
            new DiscoveredModelEntity(
                uuid, runtimeId, modelId, null, ResourceRole.VISUAL_PART, eventId));
    assertThrows(
        NullPointerException.class,
        () -> new DiscoveredModelEntity(uuid, runtimeId, modelId, "part_head", null, eventId));
  }

  @Test
  void testPlatformCapabilityAndDescriptor() {
    assertEquals(6, PlatformCapability.values().length);
    for (PlatformCapability cap : PlatformCapability.values()) {
      assertNotNull(cap.name());
    }

    PlatformDescriptor desc =
        new PlatformDescriptor("paper", "PaperServer", "26.1.0", "26.1.0-R0.1-SNAPSHOT");
    assertEquals("paper", desc.family());
    assertEquals("PaperServer", desc.implementation());
    assertEquals("26.1.0", desc.minecraftVersion());
    assertEquals("26.1.0-R0.1-SNAPSHOT", desc.apiVersion());
  }

  @Test
  void testIntegrationInitializationContext() {
    IntegrationInitializationContext ctx =
        new IntegrationInitializationContext(null, null, Logger.getLogger("test"));
    assertEquals(Logger.getLogger("test"), ctx.logger());
  }
}
