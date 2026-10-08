package io.github.kizio806.spectraevents.application.asset;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class AssetTargetProfileTest {
  @Test
  void resolvesEachSupportedReleaseLine() {
    Assertions.assertEquals(
        AssetTargetProfile.PROFILE_26_1, AssetTargetProfile.forMinecraftVersion("26.1.4"));
    Assertions.assertEquals(
        AssetTargetProfile.PROFILE_26_2, AssetTargetProfile.forMinecraftVersion("26.2"));
    Assertions.assertEquals(
        AssetTargetProfile.PROFILE_26_3, AssetTargetProfile.forMinecraftVersion("26.3.1"));
  }

  @Test
  void rejectsUnknownOrMissingVersions() {
    Assertions.assertThrows(
        IllegalArgumentException.class, () -> AssetTargetProfile.forMinecraftVersion("26.4"));
    Assertions.assertThrows(
        IllegalArgumentException.class, () -> AssetTargetProfile.forMinecraftVersion(" "));
  }

  @Test
  void exposesStableModrinthReleaseLineAndVersion() {
    Assertions.assertEquals("26.1", AssetTargetProfile.PROFILE_26_1.minecraftReleaseLine());
    Assertions.assertEquals(
        "1.2.3+26.3", AssetTargetProfile.PROFILE_26_3.resourcePackVersion("1.2.3"));
  }
}
