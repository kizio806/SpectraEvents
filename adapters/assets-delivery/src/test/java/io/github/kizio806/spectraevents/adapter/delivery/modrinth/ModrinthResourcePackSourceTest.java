package io.github.kizio806.spectraevents.adapter.delivery.modrinth;

import io.github.kizio806.spectraevents.application.asset.AssetTargetProfile;
import io.github.kizio806.spectraevents.application.asset.delivery.ResourcePackDescriptor;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class ModrinthResourcePackSourceTest {

  private static class StubClient extends ModrinthApiClient {
    private final String fixture;

    public StubClient(String fixture) {
      super("1.0");
      this.fixture = fixture;
    }

    @Override
    public CompletableFuture<String> getProjectVersions(
        String projectId, String loader, String gameVersion) {
      if ("26.3".equals(gameVersion) && "[]".equals(fixture)) {
        return CompletableFuture.completedFuture("[]");
      }
      return CompletableFuture.completedFuture(fixture);
    }
  }

  @Test
  void testExactVersionSelected() throws Exception {
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
                  "sha1": "abc",
                  "sha512": "def"
                }
              }
            ]
          }
        ]
        """;
    StubClient mockClient = new StubClient(fixture);

    ModrinthResourcePackSource source = new ModrinthResourcePackSource(mockClient, "xyz", "26.1.1");
    ResourcePackDescriptor desc = source.resolve("1.0.0", AssetTargetProfile.PROFILE_26_1).get();

    Assertions.assertEquals("1.0.0+26.1", desc.version());
    Assertions.assertEquals("https://cdn.modrinth.com/data/xyz/versions/v1/pack.zip", desc.url());
    Assertions.assertEquals("abc", desc.sha1());
    Assertions.assertEquals("def", desc.sha512());
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
              source.resolve("1.0.0", AssetTargetProfile.PROFILE_26_3).get();
            });
    Assertions.assertTrue(ex.getCause().getMessage().contains("not found"));
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
                  "sha1": "abc"
                }
              }
            ]
          }
        ]
        """;
    StubClient mockClient = new StubClient(fixture);

    ModrinthResourcePackSource source = new ModrinthResourcePackSource(mockClient, "xyz", "26.1.1");
    ExecutionException ex =
        Assertions.assertThrows(
            ExecutionException.class,
            () -> {
              source.resolve("1.0.0", AssetTargetProfile.PROFILE_26_1).get();
            });
    Assertions.assertTrue(ex.getCause().getMessage().contains("invalid"));
  }
}
