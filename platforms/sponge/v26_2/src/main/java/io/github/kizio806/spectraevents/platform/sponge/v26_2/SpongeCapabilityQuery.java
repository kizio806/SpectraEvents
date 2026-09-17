package io.github.kizio806.spectraevents.platform.sponge.v26_2;

import io.github.kizio806.spectraevents.application.port.PlatformCapability;
import io.github.kizio806.spectraevents.application.port.PlatformCapabilityQuery;

public class SpongeCapabilityQuery implements PlatformCapabilityQuery {
  @Override
  public boolean hasCapability(PlatformCapability capability) {
    return switch (capability) {
      case DISPLAY_ENTITIES -> true;
      case INTERACTION_ENTITIES -> true;
      case REGION_SCHEDULING -> false;
      case ADMIN_GUI -> false;
      case ADVENTURE_NATIVE -> true;
      case CUSTOM_ITEMS -> false;
      default -> false;
    };
  }

  @Override
  public io.github.kizio806.spectraevents.application.port.PlatformDescriptor platformDescriptor() {
    return new io.github.kizio806.spectraevents.application.port.PlatformDescriptor(
        "Sponge",
        org.spongepowered.api.Sponge.platform()
            .container(org.spongepowered.api.Platform.Component.IMPLEMENTATION)
            .metadata()
            .name()
            .orElse("Sponge"),
        org.spongepowered.api.Sponge.platform().minecraftVersion().name(),
        org.spongepowered.api.Sponge.platform()
            .container(org.spongepowered.api.Platform.Component.API)
            .metadata()
            .version()
            .toString());
  }
}
