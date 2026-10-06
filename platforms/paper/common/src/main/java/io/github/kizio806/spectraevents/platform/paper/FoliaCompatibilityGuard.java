package io.github.kizio806.spectraevents.platform.paper;

import io.papermc.paper.ServerBuildInfo;
import net.kyori.adventure.key.Key;

/** Refuses Folia releases that are outside this distribution's verified compatibility band. */
final class FoliaCompatibilityGuard {
  private static final Key FOLIA_BRAND = Key.key("papermc", "folia");

  private FoliaCompatibilityGuard() {}

  static boolean shouldRefuse(ServerBuildInfo buildInfo) {
    return buildInfo.isBrandCompatible(FOLIA_BRAND)
        && !isSupportedFoliaMinecraftVersion(buildInfo.minecraftVersionId());
  }

  static boolean isSupportedFoliaMinecraftVersion(String minecraftVersion) {
    return minecraftVersion.matches("26\\.(1|2)(?:\\.\\d+)?");
  }
}
