package io.github.kizio806.spectraevents.adapter.delivery.manual;

import io.github.kizio806.spectraevents.application.asset.AssetTargetProfile;
import io.github.kizio806.spectraevents.application.asset.delivery.ResourcePackDescriptor;
import java.util.concurrent.ExecutionException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class ManualUrlResourcePackSourceTest {

  private static final String TEST_VERSION = "1.0.0";

  @Test
  void testValidManualSource()
      throws InterruptedException, java.util.concurrent.ExecutionException {
    ManualUrlResourcePackSource source =
        new ManualUrlResourcePackSource("https://example.com/pack.zip", "a".repeat(40));

    ResourcePackDescriptor desc =
        source.resolve(TEST_VERSION, AssetTargetProfile.PROFILE_26_3).get();
    Assertions.assertEquals("https://example.com/pack.zip", desc.url());
    Assertions.assertEquals("a".repeat(40), desc.sha1());
    Assertions.assertEquals("MANUAL", desc.source());
  }

  @Test
  void testMissingUrlRejected() {
    ManualUrlResourcePackSource source = new ManualUrlResourcePackSource("", "a".repeat(40));
    ExecutionException ex =
        Assertions.assertThrows(
            ExecutionException.class,
            () -> {
              source.resolve(TEST_VERSION, AssetTargetProfile.PROFILE_26_3).get();
            });
    Assertions.assertTrue(ex.getCause().getMessage().contains("empty"));
  }

  @Test
  void testMissingSha1Rejected() {
    ManualUrlResourcePackSource source =
        new ManualUrlResourcePackSource("https://example.com/pack.zip", "");
    ExecutionException ex =
        Assertions.assertThrows(
            ExecutionException.class,
            () -> {
              source.resolve(TEST_VERSION, AssetTargetProfile.PROFILE_26_3).get();
            });
    Assertions.assertTrue(ex.getCause().getMessage().contains("exactly 40"));
  }

  @Test
  void testHttpUrlRejected() {
    ManualUrlResourcePackSource source =
        new ManualUrlResourcePackSource("http://example.com/pack.zip", "a".repeat(40));
    ExecutionException ex =
        Assertions.assertThrows(
            ExecutionException.class,
            () -> {
              source.resolve(TEST_VERSION, AssetTargetProfile.PROFILE_26_3).get();
            });
    Assertions.assertTrue(ex.getCause().getMessage().contains("HTTPS"));
  }
}
