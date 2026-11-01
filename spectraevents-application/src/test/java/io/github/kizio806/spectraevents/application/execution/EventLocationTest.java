package io.github.kizio806.spectraevents.application.execution;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;
import org.junit.jupiter.api.Test;

class EventLocationTest {

  @Test
  void testSerializeAndDeserialize() {
    EventLocation loc = new EventLocation("world_nether", 123.5, 64.0, -456.75, 90.0f, -45.0f);
    String serialized = loc.serialize();
    assertTrue(serialized.startsWith("v1|world_nether|"));

    Optional<EventLocation> deserialized = EventLocation.deserialize(serialized);
    assertTrue(deserialized.isPresent());
    assertEquals(loc, deserialized.get());
  }

  @Test
  void testDeserializeMalformed() {
    assertFalse(EventLocation.deserialize(null).isPresent());
    assertFalse(EventLocation.deserialize("").isPresent());
    assertFalse(EventLocation.deserialize("v2|world|1|2|3|0|0").isPresent());
    assertFalse(EventLocation.deserialize("v1|world|1|2|3").isPresent());
    assertFalse(EventLocation.deserialize("v1|world|not_a_double|2|3|0|0").isPresent());
  }

  @Test
  void testValidationInvariants() {
    assertThrows(NullPointerException.class, () -> new EventLocation(null, 0, 0, 0, 0, 0));
    assertThrows(IllegalArgumentException.class, () -> new EventLocation("", 0, 0, 0, 0, 0));
    assertThrows(
        IllegalArgumentException.class, () -> new EventLocation("world|pipe", 0, 0, 0, 0, 0));
    assertThrows(
        IllegalArgumentException.class, () -> new EventLocation("world", Double.NaN, 0, 0, 0, 0));
    assertThrows(
        IllegalArgumentException.class,
        () -> new EventLocation("world", 0, Double.POSITIVE_INFINITY, 0, 0, 0));
    assertThrows(
        IllegalArgumentException.class,
        () -> new EventLocation("world", 0, 0, Double.NEGATIVE_INFINITY, 0, 0));
  }
}
