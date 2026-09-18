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
}
