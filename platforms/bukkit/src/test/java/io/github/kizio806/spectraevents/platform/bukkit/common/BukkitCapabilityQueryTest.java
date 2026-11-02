package io.github.kizio806.spectraevents.platform.bukkit.common;

import static org.junit.jupiter.api.Assertions.assertThrows;

import io.github.kizio806.spectraevents.application.port.PlatformCapability;
import org.junit.jupiter.api.Test;

class BukkitCapabilityQueryTest {

  @Test
  void rejectsNullPlatformName() {
    assertThrows(
        NullPointerException.class,
        () ->
            new BukkitCapabilityQuery(null) {
              @Override
              public boolean hasCapability(PlatformCapability capability) {
                return false;
              }
            });
  }
}
