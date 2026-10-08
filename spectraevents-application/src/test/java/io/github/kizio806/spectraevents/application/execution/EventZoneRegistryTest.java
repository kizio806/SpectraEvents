package io.github.kizio806.spectraevents.application.execution;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.github.kizio806.spectraevents.core.event.definition.EventDefinitionId;
import io.github.kizio806.spectraevents.core.event.runtime.EventInstanceId;
import org.junit.jupiter.api.Test;

class EventZoneRegistryTest {
  @Test
  void acceptsThreeSeparatedZonesButRejectsOverlapAndFourthZoneInWorld() {
    EventZoneRegistry registry = new EventZoneRegistry();
    registry.reserve(EventInstanceId.generate(), new EventZone("world", 0, 0, 50));
    registry.reserve(EventInstanceId.generate(), new EventZone("world", 200, 0, 50));
    registry.reserve(EventInstanceId.generate(), new EventZone("world", 400, 0, 50));

    assertThrows(
        IllegalStateException.class,
        () -> registry.reserve(EventInstanceId.generate(), new EventZone("world", 600, 0, 50)));
    assertDoesNotThrow(
        () ->
            registry.reserve(EventInstanceId.generate(), new EventZone("world_nether", 20, 0, 50)));
    assertDoesNotThrow(
        () ->
            registry.reserve(
                EventInstanceId.generate(), new EventZone("another_world", 20, 0, 50)));
  }

  @Test
  void appliesCapacityPerDefinitionInsteadOfAHiddenWorldWideLimit() {
    EventZoneRegistry registry = new EventZoneRegistry();
    registry.reserve(
        EventInstanceId.generate(),
        new EventDefinitionId("metin"),
        new EventZone("world", 0, 0, 20),
        1);
    registry.reserve(
        EventInstanceId.generate(),
        new EventDefinitionId("airdrop"),
        new EventZone("world", 100, 0, 20),
        1);
    registry.reserve(
        EventInstanceId.generate(),
        new EventDefinitionId("meteor"),
        new EventZone("world", 200, 0, 20),
        1);

    assertThrows(
        IllegalStateException.class,
        () ->
            registry.reserve(
                EventInstanceId.generate(),
                new EventDefinitionId("metin"),
                new EventZone("world", 300, 0, 20),
                1));
  }
}
