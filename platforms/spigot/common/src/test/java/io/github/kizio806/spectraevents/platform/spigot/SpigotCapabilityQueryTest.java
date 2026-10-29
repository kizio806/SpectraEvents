package io.github.kizio806.spectraevents.platform.spigot;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.kizio806.spectraevents.application.port.PlatformCapability;
import org.junit.jupiter.api.Test;

class SpigotCapabilityQueryTest {

  @Test
  void reportsCorrectCapabilitiesForSpigotPlatform() {
    SpigotCapabilityQuery query = new SpigotCapabilityQuery();

    assertFalse(query.hasCapability(PlatformCapability.REGION_SCHEDULING));
    assertFalse(query.hasCapability(PlatformCapability.ADVENTURE_NATIVE));
    assertTrue(query.hasCapability(PlatformCapability.DISPLAY_ENTITIES));
    assertTrue(query.hasCapability(PlatformCapability.INTERACTION_ENTITIES));
    assertTrue(query.hasCapability(PlatformCapability.ADMIN_GUI));
  }
}
