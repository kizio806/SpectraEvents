package io.github.kizio806.spectraevents.platform.paper.common;

import io.github.kizio806.spectraevents.application.port.PlatformCapability;
import io.github.kizio806.spectraevents.platform.bukkit.common.BukkitCapabilityQuery;

public final class PaperCapabilityQuery extends BukkitCapabilityQuery {

  public PaperCapabilityQuery() {
    super("Paper");
  }

  @Override
  public boolean hasCapability(PlatformCapability capability) {
    return true; // Paper supports everything we need for this project.
  }
}
