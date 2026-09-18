package io.github.kizio806.spectraevents.application.asset;

import java.nio.file.Files;
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
    builder.build(Collections.emptyList(), AssetTargetProfile.PROFILE_26_1);

    Path mcmeta =
        Files.walk(tempDir).filter(p -> p.endsWith("pack.mcmeta")).findFirst().orElseThrow();
    String content = Files.readString(mcmeta);
    Assertions.assertTrue(content.contains("\"pack_format\": 84"), "Expected pack format 84");
  }

  @Test
  void testBuildForProfile26_2() throws Exception {
    ResourcePackBuilder builder = new ResourcePackBuilder(tempDir);
    builder.build(Collections.emptyList(), AssetTargetProfile.PROFILE_26_2);

    Path mcmeta =
        Files.walk(tempDir).filter(p -> p.endsWith("pack.mcmeta")).findFirst().orElseThrow();
    String content = Files.readString(mcmeta);
    Assertions.assertTrue(content.contains("\"pack_format\": 88"), "Expected pack format 88");
  }

  @Test
  void testBuildForProfile26_3() throws Exception {
    ResourcePackBuilder builder = new ResourcePackBuilder(tempDir);
    builder.build(Collections.emptyList(), AssetTargetProfile.PROFILE_26_3);

    Path mcmeta =
        Files.walk(tempDir).filter(p -> p.endsWith("pack.mcmeta")).findFirst().orElseThrow();
    String content = Files.readString(mcmeta);
    Assertions.assertTrue(content.contains("\"pack_format\": 97"), "Expected pack format 97");
  }
}
