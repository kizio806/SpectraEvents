package io.github.kizio806.spectraevents.application.asset;

import java.nio.file.Path;
import java.util.Collections;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public class ResourcePackBuilderTest {

  @TempDir Path tempDir;

  @Test
  void testBuildForProfile26_1() throws Exception {
    ResourcePackBuilder builder = new ResourcePackBuilder(tempDir);
    UnsupportedOperationException error =
        Assertions.assertThrows(
            UnsupportedOperationException.class,
            () -> builder.build(Collections.emptyList(), AssetTargetProfile.PROFILE_26_1));
    Assertions.assertTrue(error.getMessage().contains("unavailable"));
  }

  @Test
  void testBuildForProfile26_2() throws Exception {
    ResourcePackBuilder builder = new ResourcePackBuilder(tempDir);
    Assertions.assertThrows(
        UnsupportedOperationException.class,
        () -> builder.build(Collections.emptyList(), AssetTargetProfile.PROFILE_26_2));
  }

  @Test
  void testBuildForProfile26_3() throws Exception {
    ResourcePackBuilder builder = new ResourcePackBuilder(tempDir);
    Assertions.assertThrows(
        UnsupportedOperationException.class,
        () -> builder.build(Collections.emptyList(), AssetTargetProfile.PROFILE_26_3));
  }
}
