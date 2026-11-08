package io.github.kizio806.spectraevents.platform.bukkit.asset.delivery;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.kizio806.spectraevents.application.asset.AssetTargetProfile;
import io.github.kizio806.spectraevents.application.asset.delivery.PlayerAssetReadiness;
import io.github.kizio806.spectraevents.application.asset.delivery.PlayerResourcePackService;
import io.github.kizio806.spectraevents.application.asset.delivery.PlayerResourcePackState;
import io.github.kizio806.spectraevents.application.asset.delivery.ResourcePackDescriptor;
import io.github.kizio806.spectraevents.application.asset.delivery.ResourcePackDescriptorCache;
import io.github.kizio806.spectraevents.application.config.locale.FileSystemLocaleLoader;
import io.github.kizio806.spectraevents.application.config.locale.LocaleCatalog;
import io.github.kizio806.spectraevents.platform.bukkit.compat.BukkitPlatformBridge;
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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

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
              if ("getName".equals(method.getName())) {
                return "MockPlayer";
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
    assertNull(adapter.mapStatus(null));
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

    PlayerJoinEvent joinEvent = BukkitPlatformBridge.createPlayerJoinEvent(player);
    adapter.onJoin(joinEvent);
    assertTrue(adapter.sent.get());
    assertEquals(PlayerResourcePackState.REQUESTED, service.getState(playerId));

    PlayerResourcePackStatusEvent packEvent =
        new PlayerResourcePackStatusEvent(
            player, UUID.randomUUID(), PlayerResourcePackStatusEvent.Status.DECLINED);
    adapter.onPackStatus(packEvent);
    assertTrue(adapter.kicked.get());
    assertEquals(PlayerResourcePackState.DECLINED, service.getState(playerId));

    PlayerQuitEvent quitEvent = BukkitPlatformBridge.createPlayerQuitEvent(player);
    adapter.onQuit(quitEvent);
    assertEquals(PlayerResourcePackState.NOT_REQUESTED, service.getState(playerId));
  }

  @ParameterizedTest
  @EnumSource(
      value = PlayerResourcePackStatusEvent.Status.class,
      names = {"ACCEPTED", "DOWNLOADED", "SUCCESSFULLY_LOADED"})
  void neverKicksPlayerOnAcceptDownloadOrLoadEvenWhenRequired(
      PlayerResourcePackStatusEvent.Status status) throws IOException {
    LocaleCatalog locales = new FileSystemLocaleLoader(tempDir).loadCatalog("en-US");
    PlayerResourcePackService service =
        new PlayerResourcePackService(new ResourcePackDescriptorCache(), true, "prompt");
    TestAdapter adapter =
        new TestAdapter(service, "1.0.0", AssetTargetProfile.PROFILE_26_1, "cfg", locales);

    UUID playerId = UUID.randomUUID();
    Player player = createMockPlayer(playerId);

    PlayerResourcePackStatusEvent event =
        new PlayerResourcePackStatusEvent(player, UUID.randomUUID(), status);
    adapter.onPackStatus(event);

    assertFalse(adapter.kicked.get());
    if (status == PlayerResourcePackStatusEvent.Status.SUCCESSFULLY_LOADED) {
      assertEquals(PlayerResourcePackState.LOADED, service.getState(playerId));
      assertEquals(PlayerAssetReadiness.READY, service.getReadiness(playerId));
    } else if (status == PlayerResourcePackStatusEvent.Status.DOWNLOADED) {
      assertEquals(PlayerResourcePackState.DOWNLOADED, service.getState(playerId));
      assertEquals(PlayerAssetReadiness.NOT_READY, service.getReadiness(playerId));
    } else {
      assertEquals(PlayerResourcePackState.ACCEPTED, service.getState(playerId));
      assertEquals(PlayerAssetReadiness.NOT_READY, service.getReadiness(playerId));
    }
  }

  @ParameterizedTest
  @EnumSource(
      value = PlayerResourcePackStatusEvent.Status.class,
      names = {"DECLINED", "FAILED_DOWNLOAD", "INVALID_URL", "FAILED_RELOAD", "DISCARDED"})
  void kicksPlayerOnFailureOrDeclineOnlyWhenRequired(PlayerResourcePackStatusEvent.Status status)
      throws IOException {
    LocaleCatalog locales = new FileSystemLocaleLoader(tempDir).loadCatalog("en-US");

    // Case 1: When required -> MUST kick
    PlayerResourcePackService requiredService =
        new PlayerResourcePackService(new ResourcePackDescriptorCache(), true, "prompt");
    TestAdapter requiredAdapter =
        new TestAdapter(requiredService, "1.0.0", AssetTargetProfile.PROFILE_26_1, "cfg", locales);

    UUID player1Id = UUID.randomUUID();
    Player player1 = createMockPlayer(player1Id);
    requiredAdapter.onPackStatus(
        new PlayerResourcePackStatusEvent(player1, UUID.randomUUID(), status));
    assertTrue(requiredAdapter.kicked.get());

    // Case 2: When NOT required -> MUST NOT kick
    PlayerResourcePackService optionalService =
        new PlayerResourcePackService(new ResourcePackDescriptorCache(), false, "prompt");
    TestAdapter optionalAdapter =
        new TestAdapter(optionalService, "1.0.0", AssetTargetProfile.PROFILE_26_1, "cfg", locales);

    UUID player2Id = UUID.randomUUID();
    Player player2 = createMockPlayer(player2Id);
    optionalAdapter.onPackStatus(
        new PlayerResourcePackStatusEvent(player2, UUID.randomUUID(), status));
    assertFalse(optionalAdapter.kicked.get());
  }

  @Test
  void safelyIgnoresNullStatusEvent() throws IOException {
    LocaleCatalog locales = new FileSystemLocaleLoader(tempDir).loadCatalog("en-US");
    PlayerResourcePackService service =
        new PlayerResourcePackService(new ResourcePackDescriptorCache(), true, "prompt");
    TestAdapter adapter =
        new TestAdapter(service, "1.0.0", AssetTargetProfile.PROFILE_26_1, "cfg", locales);

    adapter.onPackStatus(null);
    assertFalse(adapter.kicked.get());

    adapter.onQuit(null);
    adapter.onQuit(BukkitPlatformBridge.createPlayerQuitEvent(createMockPlayer(UUID.randomUUID())));
  }

  @Test
  void rejectsNullConstructorArguments() {
    assertThrows(
        NullPointerException.class,
        () -> new TestAdapter(null, "1.0.0", AssetTargetProfile.PROFILE_26_1, "cfg", null));
  }
}
