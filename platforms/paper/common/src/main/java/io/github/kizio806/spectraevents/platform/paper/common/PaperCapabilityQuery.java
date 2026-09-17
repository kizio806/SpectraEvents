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
    return new io.github.kizio806.spectraevents.application.port.PlatformDescriptor(
        "Paper",
        org.bukkit.Bukkit.getServer().getName(),
        org.bukkit.Bukkit.getBukkitVersion().split("-")[0],
        org.bukkit.Bukkit.getBukkitVersion());
  }
}
