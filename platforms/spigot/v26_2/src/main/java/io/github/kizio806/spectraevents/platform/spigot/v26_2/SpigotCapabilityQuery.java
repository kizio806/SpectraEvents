package io.github.kizio806.spectraevents.platform.spigot.v26_2;

import io.github.kizio806.spectraevents.application.port.PlatformCapability;
import io.github.kizio806.spectraevents.application.port.PlatformCapabilityQuery;

public final class SpigotCapabilityQuery implements PlatformCapabilityQuery {
  @Override
  public boolean hasCapability(PlatformCapability capability) {
    if (capability == PlatformCapability.REGION_SCHEDULING) {
      return false; // Spigot does not support Folia-style region scheduling natively
    }
    if (capability == PlatformCapability.ADVENTURE_NATIVE) {
      return false; // Spigot does not implement Adventure directly on Player
    }
    // Assume other features (DISPLAY_ENTITIES, INTERACTION_ENTITIES, CUSTOM_ITEMS, GUI) are
    // supported on 1.21.3
    return true;
  }

  @Override
  public String platformFamily() {
    return "spigot";
  }
}
