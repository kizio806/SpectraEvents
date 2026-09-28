package io.github.kizio806.spectraevents.application.asset.delivery;

import io.github.kizio806.spectraevents.application.asset.AssetTargetProfile;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class ResourcePackDeliveryCoordinatorTest {
  private static final String PLUGIN_VERSION = "0.1.0";
  private static final String CONFIG_ID = "config";

  @Test
  void refreshPublishesVerifiedDescriptorToPlayerDeliveryCache() {
    ResourcePackDescriptorCache cache = new ResourcePackDescriptorCache();
    ResourcePackDescriptor descriptor = descriptor();
    ResourcePackSourcePort source =
        (pluginVersion, profile) -> CompletableFuture.completedFuture(descriptor);
    ResourcePackDeliveryCoordinator coordinator =
        new ResourcePackDeliveryCoordinator(
            source, cache, PLUGIN_VERSION, AssetTargetProfile.PROFILE_26_1, CONFIG_ID);

    Assertions.assertEquals(descriptor, coordinator.refresh().join());
    Assertions.assertEquals(
        descriptor,
        cache.get(PLUGIN_VERSION, AssetTargetProfile.PROFILE_26_1, CONFIG_ID).orElseThrow());
  }

  @Test
  void refreshFailureRetainsLastKnownGoodDescriptorForExistingPlayers() {
    ResourcePackDescriptorCache cache = new ResourcePackDescriptorCache();
    ResourcePackDescriptor descriptor = descriptor();
    cache.put(descriptor, PLUGIN_VERSION, AssetTargetProfile.PROFILE_26_1, CONFIG_ID);
    ResourcePackSourcePort failing =
        (pluginVersion, profile) ->
            CompletableFuture.failedFuture(new IllegalStateException("network failure"));
    ResourcePackDeliveryCoordinator coordinator =
        new ResourcePackDeliveryCoordinator(
            failing, cache, PLUGIN_VERSION, AssetTargetProfile.PROFILE_26_1, CONFIG_ID);

    Assertions.assertThrows(
        java.util.concurrent.CompletionException.class, () -> coordinator.refresh().join());
    Assertions.assertEquals(
        descriptor,
        cache.get(PLUGIN_VERSION, AssetTargetProfile.PROFILE_26_1, CONFIG_ID).orElseThrow());
  }

  @Test
  void enabledSettingsRequireSecureZipAndExactHash() {
    Assertions.assertThrows(
        IllegalArgumentException.class,
        () ->
            new ResourcePackDeliverySettings(
                true, false, "http://example.com/pack.zip", "a".repeat(40), "prompt", ""));
    Assertions.assertThrows(
        IllegalArgumentException.class,
        () ->
            new ResourcePackDeliverySettings(
                true, false, "https://example.com/pack.zip", "a", "prompt", ""));
    ResourcePackDeliverySettings settings =
        new ResourcePackDeliverySettings(
            true, true, "https://example.com/pack.zip", "a".repeat(40), "prompt", "");
    Assertions.assertEquals(64, settings.sourceConfigId().length());
  }

  private static ResourcePackDescriptor descriptor() {
    return new ResourcePackDescriptor(
        "local",
        PLUGIN_VERSION,
        "https://example.com/pack.zip",
        "a".repeat(40),
        null,
        1L,
        AssetTargetProfile.PROFILE_26_1,
        "MANUAL");
  }
}
