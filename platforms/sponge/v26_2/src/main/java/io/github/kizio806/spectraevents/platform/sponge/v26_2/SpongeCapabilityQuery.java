package io.github.kizio806.spectraevents.platform.sponge.v26_2;

import io.github.kizio806.spectraevents.application.port.PlatformCapability;
import io.github.kizio806.spectraevents.application.port.PlatformCapabilityQuery;

public class SpongeCapabilityQuery implements PlatformCapabilityQuery {
  @Override
  public boolean hasCapability(PlatformCapability capability) {
    return switch (capability) {
      case DISPLAY_ENTITIES -> false;
      case INTERACTION_ENTITIES -> false;
      case REGION_SCHEDULING -> false;
      case ADMIN_GUI -> false;
      case ADVENTURE_NATIVE -> true;
      case CUSTOM_ITEMS -> false;
      default -> false;
    };
  }

  @Override
  public String platformFamily() {
    return "Sponge";
  }
}
