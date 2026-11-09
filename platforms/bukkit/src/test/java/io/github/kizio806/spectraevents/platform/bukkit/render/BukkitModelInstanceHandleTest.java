package io.github.kizio806.spectraevents.platform.bukkit.render;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.github.kizio806.spectraevents.core.event.runtime.EventInstanceId;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class BukkitModelInstanceHandleTest {

  @Test
  void constructsHandleWithValidParameters() {
    EventInstanceId instanceId = EventInstanceId.generate();
    UUID partId = UUID.randomUUID();
    UUID interactionId = UUID.randomUUID();
    Map<String, UUID> parts = Map.of("head", partId);

    BukkitModelInstanceHandle handle =
        new BukkitModelInstanceHandle(instanceId, parts, interactionId);

    assertEquals(instanceId, handle.instanceId());
    assertEquals(parts, handle.partEntities());
    assertEquals(interactionId, handle.interactionEntityId());
  }

  @Test
  void permitsNullInteractionEntityId() {
    EventInstanceId instanceId = EventInstanceId.generate();
    BukkitModelInstanceHandle handle = new BukkitModelInstanceHandle(instanceId, Map.of(), null);

    assertEquals(instanceId, handle.instanceId());
    assertEquals(Map.of(), handle.partEntities());
    assertNull(handle.interactionEntityId());
  }

  @Test
  void rejectsNullRequiredParameters() {
    EventInstanceId instanceId = EventInstanceId.generate();
    Map<String, UUID> parts = Map.of();

    assertThrows(
        NullPointerException.class, () -> new BukkitModelInstanceHandle(null, parts, null));
    assertThrows(
        NullPointerException.class, () -> new BukkitModelInstanceHandle(instanceId, null, null));
  }

  @Test
  void makesDefensiveCopyOfPartEntities() {
    EventInstanceId instanceId = EventInstanceId.generate();
    Map<String, UUID> mutableMap = new HashMap<>();
    mutableMap.put("arm", UUID.randomUUID());

    BukkitModelInstanceHandle handle = new BukkitModelInstanceHandle(instanceId, mutableMap, null);
    mutateMap(mutableMap, "leg", UUID.randomUUID());

    assertEquals(1, handle.partEntities().size());
    assertThrows(
        UnsupportedOperationException.class,
        () -> handle.partEntities().put("extra", UUID.randomUUID()));
  }

  private static <K, V> void mutateMap(Map<K, V> map, K key, V value) {
    map.put(key, value);
  }
}
