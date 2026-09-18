package io.github.kizio806.spectraevents.application.asset;

/**
 * Defines the strict resource pack target profile. 26.1.x -> RP 84 26.2 -> RP 88.0 (represented as
 * 88) 26.3 -> RP 97.1 (represented as 97)
 */
public enum AssetTargetProfile {
  PROFILE_26_1(84),
  PROFILE_26_2(88),
  PROFILE_26_3(97);

  private final int packFormat;

  AssetTargetProfile(int packFormat) {
    this.packFormat = packFormat;
  }

  public int getPackFormat() {
    return packFormat;
  }
}
