package io.github.kizio806.spectraevents.platform.paper;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class FoliaCompatibilityGuardTest {
  @Test
  void acceptsTheVerifiedFoliaMinecraftBand() {
    assertTrue(FoliaCompatibilityGuard.isSupportedFoliaMinecraftVersion("26.1.2"));
    assertTrue(FoliaCompatibilityGuard.isSupportedFoliaMinecraftVersion("26.2"));
  }

  @Test
  void rejectsTheUnavailableAndFutureFoliaMinecraftBands() {
    assertFalse(FoliaCompatibilityGuard.isSupportedFoliaMinecraftVersion("26.3"));
    assertFalse(FoliaCompatibilityGuard.isSupportedFoliaMinecraftVersion("26.4"));
  }
}
