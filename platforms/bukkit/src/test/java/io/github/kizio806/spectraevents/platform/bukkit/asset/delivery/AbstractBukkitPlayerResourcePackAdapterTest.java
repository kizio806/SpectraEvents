package io.github.kizio806.spectraevents.platform.bukkit.asset.delivery;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.github.kizio806.spectraevents.application.asset.AssetTargetProfile;
import io.github.kizio806.spectraevents.application.asset.delivery.PlayerResourcePackService;
import io.github.kizio806.spectraevents.application.asset.delivery.PlayerResourcePackState;
import io.github.kizio806.spectraevents.application.asset.delivery.ResourcePackDescriptor;
import io.github.kizio806.spectraevents.application.asset.delivery.ResourcePackDescriptorCache;
import io.github.kizio806.spectraevents.application.config.locale.FileSystemLocaleLoader;
import io.github.kizio806.spectraevents.application.config.locale.LocaleCatalog;
import java.io.IOException;
import java.nio.file.Path;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerResourcePackStatusEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class AbstractBukkitPlayerResourcePackAdapterTest {

  @TempDir Path tempDir;

  static class TestAdapter extends AbstractBukkitPlayerResourcePackAdapter {
    TestAdapter(
        PlayerResourcePackService service,
        String pluginVersion,
        AssetTargetProfile profile,
        String sourceConfigId,
        LocaleCatalog locales) {
      super(service, pluginVersion, profile, sourceConfigId, locales);
    }

    @Override
    protected void sendResourcePack(Player player, ResourcePackDescriptor pack) {}

    @Override
    protected void kickPlayer(Player player, String message) {}
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
  void rejectsNullConstructorArguments() {
    assertThrows(
        NullPointerException.class,
        () -> new TestAdapter(null, "1.0.0", AssetTargetProfile.PROFILE_26_1, "cfg", null));
  }
}
