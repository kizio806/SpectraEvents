package io.github.kizio806.spectraevents.platform.paper;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class FoliaCompatibilityGuardTest {
  @Test
  void acceptsTheVerifiedFoliaMinecraftBand() {
    assertTrue(FoliaCompatibilityGuard.isSupportedFoliaMinecraftVersion("26.1.2"));
    assertTrue(FoliaCompatibilityGuard.isSupportedFoliaMinecraftVersion("26.2"));
    assertTrue(FoliaCompatibilityGuard.isSupportedFoliaMinecraftVersion("26.3"));
    assertTrue(FoliaCompatibilityGuard.isSupportedFoliaMinecraftVersion("26.3.1"));
  }

  @Test
  void rejectsTheUnavailableAndFutureFoliaMinecraftBands() {
    assertFalse(FoliaCompatibilityGuard.isSupportedFoliaMinecraftVersion("26.0"));
    assertFalse(FoliaCompatibilityGuard.isSupportedFoliaMinecraftVersion("26.4"));
  }

  @Test
  void testShouldRefuse() {
    io.papermc.paper.ServerBuildInfo foliaSupported =
        (io.papermc.paper.ServerBuildInfo)
            java.lang.reflect.Proxy.newProxyInstance(
                io.papermc.paper.ServerBuildInfo.class.getClassLoader(),
                new Class<?>[] {io.papermc.paper.ServerBuildInfo.class},
                (proxy, method, args) -> {
                  if ("isBrandCompatible".equals(method.getName())) return true;
                  if ("minecraftVersionId".equals(method.getName())) return "26.2";
                  return null;
                });
    assertFalse(FoliaCompatibilityGuard.shouldRefuse(foliaSupported));

    io.papermc.paper.ServerBuildInfo foliaUnsupported =
        (io.papermc.paper.ServerBuildInfo)
            java.lang.reflect.Proxy.newProxyInstance(
                io.papermc.paper.ServerBuildInfo.class.getClassLoader(),
                new Class<?>[] {io.papermc.paper.ServerBuildInfo.class},
                (proxy, method, args) -> {
                  if ("isBrandCompatible".equals(method.getName())) return true;
                  if ("minecraftVersionId".equals(method.getName())) return "26.5";
                  return null;
                });
    assertTrue(FoliaCompatibilityGuard.shouldRefuse(foliaUnsupported));

    io.papermc.paper.ServerBuildInfo nonFolia =
        (io.papermc.paper.ServerBuildInfo)
            java.lang.reflect.Proxy.newProxyInstance(
                io.papermc.paper.ServerBuildInfo.class.getClassLoader(),
                new Class<?>[] {io.papermc.paper.ServerBuildInfo.class},
                (proxy, method, args) -> {
                  if ("isBrandCompatible".equals(method.getName())) return false;
                  return null;
                });
    assertFalse(FoliaCompatibilityGuard.shouldRefuse(nonFolia));
  }
}
