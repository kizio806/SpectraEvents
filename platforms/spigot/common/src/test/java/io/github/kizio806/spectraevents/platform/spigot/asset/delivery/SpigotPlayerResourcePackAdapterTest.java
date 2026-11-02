package io.github.kizio806.spectraevents.platform.spigot.asset.delivery;

import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.kizio806.spectraevents.application.asset.AssetTargetProfile;
import io.github.kizio806.spectraevents.application.asset.delivery.PlayerResourcePackService;
import io.github.kizio806.spectraevents.application.asset.delivery.ResourcePackDescriptor;
import io.github.kizio806.spectraevents.application.asset.delivery.ResourcePackDescriptorCache;
import io.github.kizio806.spectraevents.application.config.locale.FileSystemLocaleLoader;
import io.github.kizio806.spectraevents.application.config.locale.LocaleCatalog;
import java.io.IOException;
import java.lang.reflect.Proxy;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicBoolean;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class SpigotPlayerResourcePackAdapterTest {

  @TempDir Path tempDir;

  @Test
  void testSpigotPlayerResourcePackAdapter() throws IOException {
    LocaleCatalog locales = new FileSystemLocaleLoader(tempDir).loadCatalog("en-US");
    PlayerResourcePackService service =
        new PlayerResourcePackService(new ResourcePackDescriptorCache(), false, "prompt");
    SpigotPlayerResourcePackAdapter adapter =
        new SpigotPlayerResourcePackAdapter(
            service, "1.0.0", AssetTargetProfile.PROFILE_26_1, "cfg", locales);

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
                  if ("kickPlayer".equals(method.getName())) {
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
