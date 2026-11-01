package io.github.kizio806.spectraevents.application.execution;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class EventZoneTest {

  @Test
  void testConstructorValidation() {
    assertThrows(NullPointerException.class, () -> new EventZone(null, 0, 0, 10));
    assertThrows(IllegalArgumentException.class, () -> new EventZone("", 0, 0, 10));
    assertThrows(IllegalArgumentException.class, () -> new EventZone("   ", 0, 0, 10));
    assertThrows(IllegalArgumentException.class, () -> new EventZone("world", Double.NaN, 0, 10));
    assertThrows(
        IllegalArgumentException.class,
        () -> new EventZone("world", 0, Double.POSITIVE_INFINITY, 10));
    assertThrows(IllegalArgumentException.class, () -> new EventZone("world", 0, 0, Double.NaN));
    assertThrows(IllegalArgumentException.class, () -> new EventZone("world", 0, 0, 0.0));
    assertThrows(IllegalArgumentException.class, () -> new EventZone("world", 0, 0, -5.0));

    EventZone valid = new EventZone("world_the_end", 100.5, -200.5, 35.0);
    assertEquals("world_the_end", valid.world());
    assertEquals(100.5, valid.x());
    assertEquals(-200.5, valid.z());
    assertEquals(35.0, valid.radius());
  }

  @Test
  void testAtFactory() {
    EventLocation loc = new EventLocation("nether", 50.0, 64.0, 75.0, 0.0f, 0.0f);
    EventZone zone = EventZone.at(loc, 25.0);
    assertEquals("nether", zone.world());
    assertEquals(50.0, zone.x());
    assertEquals(75.0, zone.z());
    assertEquals(25.0, zone.radius());

    assertThrows(NullPointerException.class, () -> EventZone.at(null, 10.0));
  }

  @Test
  void testOverlaps() {
    EventZone zone1 = new EventZone("world", 0, 0, 10);
    EventZone zone2 = new EventZone("world", 15, 0, 10); // distance 15 < 20 -> overlaps
    EventZone zone3 = new EventZone("world", 25, 0, 10); // distance 25 > 20 -> disjoint
    EventZone zone4 = new EventZone("world_nether", 0, 0, 10); // different world

    assertTrue(zone1.overlaps(zone2));
    assertTrue(zone2.overlaps(zone1));
    assertFalse(zone1.overlaps(zone3));
    assertFalse(zone1.overlaps(zone4));
    assertThrows(NullPointerException.class, () -> zone1.overlaps(null));
  }

  @Test
  void testContains() {
    EventZone zone = new EventZone("world", 100, 200, 50);

    assertTrue(zone.contains(new EventLocation("world", 100, 64, 200, 0, 0))); // center
    assertTrue(zone.contains(new EventLocation("world", 150, 64, 200, 0, 0))); // boundary
    assertTrue(zone.contains(new EventLocation("world", 100, 64, 250, 0, 0))); // boundary
    assertTrue(
        zone.contains(
            new EventLocation("world", 130, 64, 230, 0, 0))); // inside (30^2 + 30^2 = 1800 <= 2500)

    assertFalse(zone.contains(new EventLocation("world", 151, 64, 200, 0, 0))); // outside
    assertFalse(
        zone.contains(new EventLocation("other_world", 100, 64, 200, 0, 0))); // different world
    assertThrows(NullPointerException.class, () -> zone.contains(null));
  }
}
