package io.github.kizio806.spectraevents.platform.paper.common;

import io.github.kizio806.spectraevents.application.port.PlatformCapability;
import io.github.kizio806.spectraevents.application.port.PlatformCapabilityQuery;

public final class PaperCapabilityQuery implements PlatformCapabilityQuery {
  @Override
  public boolean hasCapability(PlatformCapability capability) {
    return true; // Paper supports everything we need for this project.
  }

  @Override
  public io.github.kizio806.spectraevents.application.port.PlatformDescriptor platformDescriptor() {
    String version = org.bukkit.Bukkit.getBukkitVersion();
    int dashIndex = version.indexOf('-');
    String shortVersion = dashIndex != -1 ? version.substring(0, dashIndex) : version;

    return new io.github.kizio806.spectraevents.application.port.PlatformDescriptor(
        "Paper", org.bukkit.Bukkit.getServer().getName(), shortVersion, version);
  }
}
