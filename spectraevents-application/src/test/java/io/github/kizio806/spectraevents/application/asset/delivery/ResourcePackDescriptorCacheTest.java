package io.github.kizio806.spectraevents.application.asset.delivery;

import io.github.kizio806.spectraevents.application.asset.AssetTargetProfile;
import java.util.Optional;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class ResourcePackDescriptorCacheTest {

  @Test
  void testCachePutAndGet() {
    ResourcePackDescriptorCache cache = new ResourcePackDescriptorCache();
    ResourcePackDescriptor desc =
        new ResourcePackDescriptor(
            "id", "ver", "url", "sha1", null, 1L, AssetTargetProfile.PROFILE_26_3, "SRC");

    cache.put(desc, "1.0", AssetTargetProfile.PROFILE_26_3, "SRC");

    Optional<ResourcePackDescriptor> retrieved =
        cache.get("1.0", AssetTargetProfile.PROFILE_26_3, "SRC");
    Assertions.assertTrue(retrieved.isPresent());
    Assertions.assertEquals("id", retrieved.get().id());
  }

  @Test
  void testCacheInvalidation() {
    ResourcePackDescriptorCache cache = new ResourcePackDescriptorCache();
    ResourcePackDescriptor desc =
        new ResourcePackDescriptor(
            "id", "ver", "url", "sha1", null, 1L, AssetTargetProfile.PROFILE_26_3, "SRC");

    cache.put(desc, "1.0", AssetTargetProfile.PROFILE_26_3, "SRC");
    cache.invalidate();

    Assertions.assertTrue(cache.get("1.0", AssetTargetProfile.PROFILE_26_3, "SRC").isEmpty());
  }

  @Test
  void testLastKnownGoodMismatchedVersion() {
    ResourcePackDescriptorCache cache = new ResourcePackDescriptorCache();
    ResourcePackDescriptor desc =
        new ResourcePackDescriptor(
            "id", "ver", "url", "sha1", null, 1L, AssetTargetProfile.PROFILE_26_3, "SRC");

    cache.put(desc, "1.0", AssetTargetProfile.PROFILE_26_3, "SRC");

    Optional<ResourcePackDescriptor> lkg =
        cache.getLastKnownGood("2.0", AssetTargetProfile.PROFILE_26_3);
    Assertions.assertTrue(lkg.isEmpty());
  }
}
