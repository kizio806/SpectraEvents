package io.github.kizio806.spectraevents.platform.bukkit.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.github.kizio806.spectraevents.application.port.PlatformCapability;
import io.github.kizio806.spectraevents.application.port.PlatformDescriptor;
import io.github.kizio806.spectraevents.platform.bukkit.testutil.MockBukkitSetup;
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

  @Test
  void shouldReturnPlatformDescriptor() {
    MockBukkitSetup.ensureInitialized();
    BukkitCapabilityQuery query =
        new BukkitCapabilityQuery("TestPlatform") {
          @Override
          public boolean hasCapability(PlatformCapability capability) {
            return true;
          }
        };
    PlatformDescriptor desc = query.platformDescriptor();
    assertNotNull(desc);
    assertEquals("TestPlatform", desc.family());
    assertEquals("MockServer", desc.implementation());
    assertEquals("26.1.0", desc.minecraftVersion());
    assertEquals("26.1.0-R0.1-SNAPSHOT", desc.apiVersion());
  }
}
