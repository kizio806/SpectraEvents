package io.github.kizio806.spectraevents.platform.paper.common;

import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.kizio806.spectraevents.application.port.PlatformCapability;
import org.junit.jupiter.api.Test;

class PaperCapabilityQueryTest {

  @Test
  void reportsCorrectCapabilitiesForPaperPlatform() {
    PaperCapabilityQuery query = new PaperCapabilityQuery();

    assertTrue(query.hasCapability(PlatformCapability.REGION_SCHEDULING));
    assertTrue(query.hasCapability(PlatformCapability.ADVENTURE_NATIVE));
    assertTrue(query.hasCapability(PlatformCapability.DISPLAY_ENTITIES));
    assertTrue(query.hasCapability(PlatformCapability.INTERACTION_ENTITIES));
    assertTrue(query.hasCapability(PlatformCapability.ADMIN_GUI));
  }
}
