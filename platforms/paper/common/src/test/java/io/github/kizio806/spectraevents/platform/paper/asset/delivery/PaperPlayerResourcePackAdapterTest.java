package io.github.kizio806.spectraevents.platform.paper.asset.delivery;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.kizio806.spectraevents.application.asset.AssetTargetProfile;
import io.github.kizio806.spectraevents.application.asset.delivery.PlayerResourcePackService;
import io.github.kizio806.spectraevents.application.asset.delivery.PlayerResourcePackState;
import io.github.kizio806.spectraevents.application.asset.delivery.ResourcePackDescriptor;
import io.github.kizio806.spectraevents.application.asset.delivery.ResourcePackDescriptorCache;
import io.github.kizio806.spectraevents.application.config.locale.FileSystemLocaleLoader;
import io.github.kizio806.spectraevents.application.config.locale.LocaleCatalog;
import java.io.IOException;
import java.lang.reflect.Proxy;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicBoolean;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerResourcePackStatusEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class PaperPlayerResourcePackAdapterTest {

  @TempDir Path tempDir;

  @Test
  void testPaperPlayerResourcePackAdapter() throws IOException {
    LocaleCatalog locales = new FileSystemLocaleLoader(tempDir).loadCatalog("en-US");
    PlayerResourcePackService service =
        new PlayerResourcePackService(new ResourcePackDescriptorCache(), false, "prompt");
    PaperPlayerResourcePackAdapter adapter =
        new PaperPlayerResourcePackAdapter(
            service, "1.0.0", AssetTargetProfile.PROFILE_26_1, "cfg", locales);

    assertEquals(
        PlayerResourcePackState.ACCEPTED,
        adapter.mapStatus(PlayerResourcePackStatusEvent.Status.ACCEPTED));
    assertEquals(
        PlayerResourcePackState.DECLINED,
        adapter.mapStatus(PlayerResourcePackStatusEvent.Status.DECLINED));
    assertEquals(
        PlayerResourcePackState.FAILED,
        adapter.mapStatus(PlayerResourcePackStatusEvent.Status.FAILED_DOWNLOAD));
    assertEquals(
        PlayerResourcePackState.LOADED,
        adapter.mapStatus(PlayerResourcePackStatusEvent.Status.SUCCESSFULLY_LOADED));
    assertEquals(
        PlayerResourcePackState.DOWNLOADED,
        adapter.mapStatus(PlayerResourcePackStatusEvent.Status.DOWNLOADED));
    assertEquals(
        PlayerResourcePackState.FAILED,
        adapter.mapStatus(PlayerResourcePackStatusEvent.Status.INVALID_URL));
    assertEquals(
        PlayerResourcePackState.FAILED,
        adapter.mapStatus(PlayerResourcePackStatusEvent.Status.FAILED_RELOAD));
    assertEquals(
        PlayerResourcePackState.DISCARDED,
        adapter.mapStatus(PlayerResourcePackStatusEvent.Status.DISCARDED));

    AtomicBoolean packSent = new AtomicBoolean(false);
    AtomicBoolean kicked = new AtomicBoolean(false);

    Player mockPlayer =
        (Player)
            Proxy.newProxyInstance(
                Player.class.getClassLoader(),
                new Class<?>[] {Player.class},
                (proxy, method, args) -> {
                  if ("setResourcePack".equals(method.getName())) {
                    packSent.set(true);
                    return null;
                  }
                  if ("kick".equals(method.getName())) {
                    kicked.set(true);
                    return null;
                  }
                  return null;
                });

    adapter.sendResourcePack(
        mockPlayer,
        new ResourcePackDescriptor(
            "test-pack",
            "1.0.0",
            "https://example.com/pack.zip",
            "da39a3ee5e6b4b0d3255bfef95601890afd80709",
            null,
            1024L,
            AssetTargetProfile.PROFILE_26_1,
            "cfg"));
    assertTrue(packSent.get());

    adapter.kickPlayer(mockPlayer, "Kicked");
    assertTrue(kicked.get());
  }
}
