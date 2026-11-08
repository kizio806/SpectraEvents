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

  @Test
  void testUrlAndHashEdgeCases() {
    // Null arguments in constructor
    ManualUrlResourcePackSource nullSource = new ManualUrlResourcePackSource(null, null);
    ExecutionException exNull =
        Assertions.assertThrows(
            ExecutionException.class,
            () -> nullSource.resolve(TEST_VERSION, AssetTargetProfile.PROFILE_26_3).get());
    Assertions.assertTrue(exNull.getCause().getMessage().contains("empty"));

    // Invalid URI syntax
    ManualUrlResourcePackSource badUriSource =
        new ManualUrlResourcePackSource("://invalid-uri", "a".repeat(40));
    ExecutionException exBadUri =
        Assertions.assertThrows(
            ExecutionException.class,
            () -> badUriSource.resolve(TEST_VERSION, AssetTargetProfile.PROFILE_26_3).get());
    Assertions.assertTrue(
        exBadUri.getCause().getMessage().contains("Invalid manual source URL format"));

    // Missing host
    ManualUrlResourcePackSource noHostSource =
        new ManualUrlResourcePackSource("https:///pack.zip", "a".repeat(40));
    ExecutionException exNoHost =
        Assertions.assertThrows(
            ExecutionException.class,
            () -> noHostSource.resolve(TEST_VERSION, AssetTargetProfile.PROFILE_26_3).get());
    Assertions.assertTrue(exNoHost.getCause().getMessage().contains("HTTPS .zip URL"));

    // UserInfo present
    ManualUrlResourcePackSource userInfoSource =
        new ManualUrlResourcePackSource("https://user:pass@example.com/pack.zip", "a".repeat(40));
    ExecutionException exUserInfo =
        Assertions.assertThrows(
            ExecutionException.class,
            () -> userInfoSource.resolve(TEST_VERSION, AssetTargetProfile.PROFILE_26_3).get());
    Assertions.assertTrue(exUserInfo.getCause().getMessage().contains("HTTPS .zip URL"));

    // Fragment present
    ManualUrlResourcePackSource fragmentSource =
        new ManualUrlResourcePackSource("https://example.com/pack.zip#frag", "a".repeat(40));
    ExecutionException exFragment =
        Assertions.assertThrows(
            ExecutionException.class,
            () -> fragmentSource.resolve(TEST_VERSION, AssetTargetProfile.PROFILE_26_3).get());
    Assertions.assertTrue(exFragment.getCause().getMessage().contains("HTTPS .zip URL"));

    // Non-zip extension
    ManualUrlResourcePackSource nonZipSource =
        new ManualUrlResourcePackSource("https://example.com/pack.tar", "a".repeat(40));
    ExecutionException exNonZip =
        Assertions.assertThrows(
            ExecutionException.class,
            () -> nonZipSource.resolve(TEST_VERSION, AssetTargetProfile.PROFILE_26_3).get());
    Assertions.assertTrue(exNonZip.getCause().getMessage().contains("HTTPS .zip URL"));

    // Invalid SHA-1 (non-hex characters or bad length)
    ManualUrlResourcePackSource badShaSource =
        new ManualUrlResourcePackSource("https://example.com/pack.zip", "g".repeat(40));
    ExecutionException exBadSha =
        Assertions.assertThrows(
            ExecutionException.class,
            () -> badShaSource.resolve(TEST_VERSION, AssetTargetProfile.PROFILE_26_3).get());
    Assertions.assertTrue(exBadSha.getCause().getMessage().contains("exactly 40 hexadecimal"));
  }
}
