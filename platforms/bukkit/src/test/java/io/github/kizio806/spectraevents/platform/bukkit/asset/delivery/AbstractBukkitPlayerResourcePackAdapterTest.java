package io.github.kizio806.spectraevents.platform.bukkit.asset.delivery;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
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
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerResourcePackStatusEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class AbstractBukkitPlayerResourcePackAdapterTest {

  @TempDir Path tempDir;

  static class TestAdapter extends AbstractBukkitPlayerResourcePackAdapter {
    final AtomicBoolean sent = new AtomicBoolean(false);
    final AtomicBoolean kicked = new AtomicBoolean(false);

    TestAdapter(
        PlayerResourcePackService service,
        String pluginVersion,
        AssetTargetProfile profile,
        String sourceConfigId,
        LocaleCatalog locales) {
      super(service, pluginVersion, profile, sourceConfigId, locales);
    }

    @Override
    protected void sendResourcePack(Player player, ResourcePackDescriptor pack) {
      sent.set(true);
    }

    @Override
    protected void kickPlayer(Player player, String message) {
      kicked.set(true);
    }
  }

  private Player createMockPlayer(UUID uuid) {
    return (Player)
        Proxy.newProxyInstance(
            Player.class.getClassLoader(),
            new Class<?>[] {Player.class},
            (proxy, method, args) -> {
              if ("getUniqueId".equals(method.getName())) {
                return uuid;
              }
              return null;
            });
  }

  @Test
  void decodesHexChecksumCorrectly() {
    byte[] decoded =
        AbstractBukkitPlayerResourcePackAdapter.hexStringToByteArray("0123456789abcdef");
    byte[] expected =
        new byte[] {0x01, 0x23, 0x45, 0x67, (byte) 0x89, (byte) 0xab, (byte) 0xcd, (byte) 0xef};
    assertArrayEquals(expected, decoded);
  }

  @Test
  void mapsStandardStatuses() throws IOException {
    LocaleCatalog locales = new FileSystemLocaleLoader(tempDir).loadCatalog("en-US");
    PlayerResourcePackService service =
        new PlayerResourcePackService(new ResourcePackDescriptorCache(), false, "prompt");
    TestAdapter adapter =
        new TestAdapter(service, "1.0.0", AssetTargetProfile.PROFILE_26_1, "cfg", locales);

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
  }

  @Test
  void testLifecycleEvents() throws IOException {
    LocaleCatalog locales = new FileSystemLocaleLoader(tempDir).loadCatalog("en-US");
    ResourcePackDescriptorCache cache = new ResourcePackDescriptorCache();
    ResourcePackDescriptor descriptor =
        new ResourcePackDescriptor(
            "test-pack",
            "1.0.0",
            "https://example.com/pack.zip",
            "da39a3ee5e6b4b0d3255bfef95601890afd80709",
            null,
            1024L,
            AssetTargetProfile.PROFILE_26_1,
            "cfg");
    cache.put(descriptor, "1.0.0", AssetTargetProfile.PROFILE_26_1, "cfg");
    PlayerResourcePackService service = new PlayerResourcePackService(cache, true, "prompt");
    TestAdapter adapter =
        new TestAdapter(service, "1.0.0", AssetTargetProfile.PROFILE_26_1, "cfg", locales);

    UUID playerId = UUID.randomUUID();
    Player player = createMockPlayer(playerId);

    PlayerJoinEvent joinEvent = new PlayerJoinEvent(player, "joined");
    adapter.onJoin(joinEvent);
    assertTrue(adapter.sent.get());
    assertEquals(PlayerResourcePackState.REQUESTED, service.getState(playerId));

    PlayerResourcePackStatusEvent packEvent =
        new PlayerResourcePackStatusEvent(
            player, UUID.randomUUID(), PlayerResourcePackStatusEvent.Status.DECLINED);
    adapter.onPackStatus(packEvent);
    assertTrue(adapter.kicked.get());
    assertEquals(PlayerResourcePackState.DECLINED, service.getState(playerId));

    PlayerQuitEvent quitEvent = new PlayerQuitEvent(player, "left");
    adapter.onQuit(quitEvent);
    assertEquals(PlayerResourcePackState.NOT_REQUESTED, service.getState(playerId));
  }

  @Test
  void rejectsNullConstructorArguments() {
    assertThrows(
        NullPointerException.class,
        () -> new TestAdapter(null, "1.0.0", AssetTargetProfile.PROFILE_26_1, "cfg", null));
  }
}
