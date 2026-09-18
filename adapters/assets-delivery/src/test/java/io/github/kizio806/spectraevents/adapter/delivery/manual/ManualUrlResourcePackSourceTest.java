package io.github.kizio806.spectraevents.adapter.delivery.manual;

import io.github.kizio806.spectraevents.application.asset.AssetTargetProfile;
import io.github.kizio806.spectraevents.application.asset.delivery.ResourcePackDescriptor;
import java.util.concurrent.ExecutionException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class ManualUrlResourcePackSourceTest {

  @Test
  void testValidManualSource() throws Exception {
    ManualUrlResourcePackSource source =
        new ManualUrlResourcePackSource("https://example.com/pack.zip", "abcdef1234567890");

    ResourcePackDescriptor desc = source.resolve("1.0.0", AssetTargetProfile.PROFILE_26_3).get();
    Assertions.assertEquals("https://example.com/pack.zip", desc.url());
    Assertions.assertEquals("abcdef1234567890", desc.sha1());
    Assertions.assertEquals("MANUAL", desc.source());
  }

  @Test
  void testMissingUrlRejected() {
    ManualUrlResourcePackSource source = new ManualUrlResourcePackSource("", "abcdef");
    ExecutionException ex =
        Assertions.assertThrows(
            ExecutionException.class,
            () -> {
              source.resolve("1.0.0", AssetTargetProfile.PROFILE_26_3).get();
            });
    Assertions.assertTrue(ex.getCause().getMessage().contains("empty"));
  }

  @Test
  void testMissingSha1Rejected() {
    ManualUrlResourcePackSource source = new ManualUrlResourcePackSource("https://example.com", "");
    ExecutionException ex =
        Assertions.assertThrows(
            ExecutionException.class,
            () -> {
              source.resolve("1.0.0", AssetTargetProfile.PROFILE_26_3).get();
            });
    Assertions.assertTrue(ex.getCause().getMessage().contains("missing"));
  }

  @Test
  void testHttpUrlRejected() {
    ManualUrlResourcePackSource source =
        new ManualUrlResourcePackSource("http://example.com", "abc");
    ExecutionException ex =
        Assertions.assertThrows(
            ExecutionException.class,
            () -> {
              source.resolve("1.0.0", AssetTargetProfile.PROFILE_26_3).get();
            });
    Assertions.assertTrue(ex.getCause().getMessage().contains("HTTPS"));
  }
}
