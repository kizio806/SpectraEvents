package io.github.kizio806.spectraevents.adapter.delivery.modrinth;

import io.github.kizio806.spectraevents.application.asset.AssetTargetProfile;
import io.github.kizio806.spectraevents.application.asset.delivery.ResourcePackDescriptor;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class ModrinthResourcePackSourceTest {

  private static final String TEST_MINECRAFT_VERSION = "26.1.1";
  private static final String TEST_PLUGIN_VERSION = "1.0.0";

  private static class StubClient extends ModrinthApiClient {
    private final String fixture;
    private String requestedGameVersion;

    StubClient(String fixture) {
      super("1.0");
      this.fixture = fixture;
    }

    @Override
    public CompletableFuture<String> getProjectVersions(
        String projectId, String loader, String gameVersion) {
      requestedGameVersion = gameVersion;
      if ("26.3".equals(gameVersion) && "[]".equals(fixture)) {
        return CompletableFuture.completedFuture("[]");
      }
      return CompletableFuture.completedFuture(fixture);
    }
  }

  @Test
  void testExactVersionSelected()
      throws InterruptedException, java.util.concurrent.ExecutionException {
    String fixture =
        """
        [
          {
            "id": "v1",
            "version_number": "1.0.0+26.1",
            "files": [
              {
                "url": "https://cdn.modrinth.com/data/xyz/versions/v1/pack.zip",
                "primary": true,
                "size": 1024,
                "hashes": {
                  "sha1": "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa",
                  "sha512": "dddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddd"
                }
              }
            ]
          }
        ]
        """;
    StubClient mockClient = new StubClient(fixture);

    ModrinthResourcePackSource source =
        new ModrinthResourcePackSource(mockClient, "xyz", TEST_MINECRAFT_VERSION);
    ResourcePackDescriptor desc =
        source.resolve(TEST_PLUGIN_VERSION, AssetTargetProfile.PROFILE_26_1).get();

    Assertions.assertEquals("1.0.0+26.1", desc.version());
    Assertions.assertEquals("https://cdn.modrinth.com/data/xyz/versions/v1/pack.zip", desc.url());
    Assertions.assertEquals("a".repeat(40), desc.sha1());
    Assertions.assertEquals("d".repeat(128), desc.sha512());
    Assertions.assertEquals("26.1", mockClient.requestedGameVersion);
  }

  @Test
  void testMissingVersionRejected() {
    String fixture = "[]";
    StubClient mockClient = new StubClient(fixture);

    ModrinthResourcePackSource source = new ModrinthResourcePackSource(mockClient, "xyz", "26.3");
    ExecutionException ex =
        Assertions.assertThrows(
            ExecutionException.class,
            () -> {
              source.resolve(TEST_PLUGIN_VERSION, AssetTargetProfile.PROFILE_26_3).get();
            });
    Assertions.assertTrue(ex.getCause().getMessage().contains("not found"));
  }

  @Test
  void selectsThePinnedVersionIdInsteadOfTheReleaseVersionName() throws Exception {
    String fixture =
        """
        [
          {
            "id": "unapproved",
            "version_number": "1.0.0+26.1",
            "files": [{"url": "https://cdn.modrinth.com/unapproved.zip", "primary": true, "size": 1, "hashes": {"sha1": "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"}}]
          },
          {
            "id": "approved",
            "version_number": "different-release-name",
            "files": [{"url": "https://cdn.modrinth.com/approved.zip", "primary": true, "size": 1, "hashes": {"sha1": "bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb"}}]
          }
        ]
        """;

    ResourcePackDescriptor descriptor =
        new ModrinthResourcePackSource(
                new StubClient(fixture), "xyz", TEST_MINECRAFT_VERSION, "approved")
            .resolve(TEST_PLUGIN_VERSION, AssetTargetProfile.PROFILE_26_1)
            .get();

    Assertions.assertEquals("approved", descriptor.id());
    Assertions.assertEquals("https://cdn.modrinth.com/approved.zip", descriptor.url());
    Assertions.assertEquals("different-release-name", descriptor.version());
  }

  @Test
  void rejectsAProfileThatDoesNotMatchTheRunningServer() {
    ModrinthResourcePackSource source =
        new ModrinthResourcePackSource(new StubClient("[]"), "xyz", TEST_MINECRAFT_VERSION);

    ExecutionException exception =
        Assertions.assertThrows(
            ExecutionException.class,
            () -> source.resolve(TEST_PLUGIN_VERSION, AssetTargetProfile.PROFILE_26_2).get());

    Assertions.assertTrue(exception.getCause().getMessage().contains("does not match"));
  }

  @Test
  void testInvalidFileUrlRejected() {
    String fixture =
        """
        [
          {
            "id": "v1",
            "version_number": "1.0.0+26.1",
            "files": [
              {
                "url": "http://cdn.modrinth.com/data/xyz/versions/v1/pack.jar",
                "primary": true,
                "size": 1024,
                "hashes": {
                  "sha1": "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
                }
              }
            ]
          }
        ]
        """;
    StubClient mockClient = new StubClient(fixture);

    ModrinthResourcePackSource source =
        new ModrinthResourcePackSource(mockClient, "xyz", TEST_MINECRAFT_VERSION);
    ExecutionException ex =
        Assertions.assertThrows(
            ExecutionException.class,
            () -> {
              source.resolve(TEST_PLUGIN_VERSION, AssetTargetProfile.PROFILE_26_1).get();
            });
    Assertions.assertTrue(ex.getCause().getMessage().contains("invalid"));
  }

  @Test
  void testUnconfiguredProjectIdRejected() {
    StubClient mockClient = new StubClient("[]");
    Assertions.assertThrows(
        IllegalArgumentException.class,
        () -> new ModrinthResourcePackSource(mockClient, "<PROJECT_ID>", "26.1.1"));
    Assertions.assertThrows(
        IllegalArgumentException.class,
        () -> new ModrinthResourcePackSource(mockClient, "", "26.1.1"));
  }
}
