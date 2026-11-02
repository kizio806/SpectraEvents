package io.github.kizio806.spectraevents.platform.bukkit.common;

import io.github.kizio806.spectraevents.application.port.PlatformCapabilityQuery;
import io.github.kizio806.spectraevents.application.port.PlatformDescriptor;
import java.util.Objects;
import org.bukkit.Bukkit;

/** Base capability query implementation for Bukkit-derived server platforms. */
public abstract class BukkitCapabilityQuery implements PlatformCapabilityQuery {

  private final String platformName;

  protected BukkitCapabilityQuery(String platformName) {
    this.platformName = Objects.requireNonNull(platformName, "platformName");
  }

  @Override
  public PlatformDescriptor platformDescriptor() {
    String version = Bukkit.getBukkitVersion();
    int dashIndex = version.indexOf('-');
    String shortVersion = dashIndex > 0 ? version.substring(0, dashIndex) : version;

    return new PlatformDescriptor(
        platformName, Bukkit.getServer().getName(), shortVersion, version);
  }
}
