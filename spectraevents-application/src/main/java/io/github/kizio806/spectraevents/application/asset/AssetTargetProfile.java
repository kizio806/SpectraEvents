package io.github.kizio806.spectraevents.application.asset;

/**
 * Defines the strict resource pack target profile:
 *
 * <ul>
 *   <li>Minecraft 26.1.x -> RP min_format: [84, 0], max_format: [84, 0]
 *   <li>Minecraft 26.2 -> RP min_format: [88, 0], max_format: [88, 0]
 *   <li>Minecraft 26.3 -> RP min_format: [97, 1], max_format: [97, 1]
 * </ul>
 */
public enum AssetTargetProfile {
  PROFILE_26_1(84, 0),
  PROFILE_26_2(88, 0),
  PROFILE_26_3(97, 1);

  private final int majorFormat;
  private final int minorFormat;

  AssetTargetProfile(int majorFormat, int minorFormat) {
    this.majorFormat = majorFormat;
    this.minorFormat = minorFormat;
  }

  public int getMajorFormat() {
    return majorFormat;
  }

  public int getMinorFormat() {
    return minorFormat;
  }

  /** Returns the Modrinth/Minecraft release line represented by this pack format. */
  public String minecraftReleaseLine() {
    return name().replace("PROFILE_", "").replace('_', '.');
  }

  /** Returns the deterministic Modrinth version number for this target profile. */
  public String resourcePackVersion(String pluginVersion) {
    if (pluginVersion == null || pluginVersion.isBlank()) {
      throw new IllegalArgumentException("Plugin version must not be blank");
    }
    return pluginVersion.trim() + "+" + minecraftReleaseLine();
  }

  /** Resolves the exact resource-pack format supported by a declared Minecraft release line. */
  public static AssetTargetProfile forMinecraftVersion(String minecraftVersion) {
    if (minecraftVersion == null || minecraftVersion.isBlank()) {
      throw new IllegalArgumentException("Minecraft version must not be blank");
    }
    if (minecraftVersion.equals("26.1") || minecraftVersion.startsWith("26.1.")) {
      return PROFILE_26_1;
    }
    if (minecraftVersion.equals("26.2") || minecraftVersion.startsWith("26.2.")) {
      return PROFILE_26_2;
    }
    if (minecraftVersion.equals("26.3") || minecraftVersion.startsWith("26.3.")) {
      return PROFILE_26_3;
    }
    throw new IllegalArgumentException(
        "Unsupported Minecraft version for resource-pack output: " + minecraftVersion);
  }
}
