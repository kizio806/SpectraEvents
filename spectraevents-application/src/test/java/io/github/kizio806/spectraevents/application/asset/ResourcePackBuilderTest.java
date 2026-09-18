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

    org.yaml.snakeyaml.Yaml yaml = new org.yaml.snakeyaml.Yaml();
    java.util.Map<String, Object> root = yaml.load(content);
    java.util.Map<String, Object> pack = (java.util.Map<String, Object>) root.get("pack");
    Assertions.assertEquals(84, pack.get("pack_format"));

    java.util.Map<String, Object> formats =
        (java.util.Map<String, Object>) pack.get("supported_formats");
    java.util.List<Integer> minFormat = (java.util.List<Integer>) formats.get("min_format");
    java.util.List<Integer> maxFormat = (java.util.List<Integer>) formats.get("max_format");

    Assertions.assertEquals(2, minFormat.size());
    Assertions.assertEquals(84, minFormat.get(0));
    Assertions.assertEquals(0, minFormat.get(1));

    Assertions.assertEquals(2, maxFormat.size());
    Assertions.assertEquals(84, maxFormat.get(0));
    Assertions.assertEquals(0, maxFormat.get(1));
  }

  @Test
  void testBuildForProfile26_2() throws Exception {
    ResourcePackBuilder builder = new ResourcePackBuilder(tempDir);
    builder.build(Collections.emptyList(), AssetTargetProfile.PROFILE_26_2);

    Path mcmeta =
        Files.walk(tempDir).filter(p -> p.endsWith("pack.mcmeta")).findFirst().orElseThrow();
    String content = Files.readString(mcmeta);

    org.yaml.snakeyaml.Yaml yaml = new org.yaml.snakeyaml.Yaml();
    java.util.Map<String, Object> root = yaml.load(content);
    java.util.Map<String, Object> pack = (java.util.Map<String, Object>) root.get("pack");
    Assertions.assertEquals(88, pack.get("pack_format"));

    java.util.Map<String, Object> formats =
        (java.util.Map<String, Object>) pack.get("supported_formats");
    java.util.List<Integer> minFormat = (java.util.List<Integer>) formats.get("min_format");
    java.util.List<Integer> maxFormat = (java.util.List<Integer>) formats.get("max_format");

    Assertions.assertEquals(2, minFormat.size());
    Assertions.assertEquals(88, minFormat.get(0));
    Assertions.assertEquals(0, minFormat.get(1));

    Assertions.assertEquals(2, maxFormat.size());
    Assertions.assertEquals(88, maxFormat.get(0));
    Assertions.assertEquals(0, maxFormat.get(1));
  }

  @Test
  void testBuildForProfile26_3() throws Exception {
    ResourcePackBuilder builder = new ResourcePackBuilder(tempDir);
    builder.build(Collections.emptyList(), AssetTargetProfile.PROFILE_26_3);

    Path mcmeta =
        Files.walk(tempDir).filter(p -> p.endsWith("pack.mcmeta")).findFirst().orElseThrow();
    String content = Files.readString(mcmeta);

    org.yaml.snakeyaml.Yaml yaml = new org.yaml.snakeyaml.Yaml();
    java.util.Map<String, Object> root = yaml.load(content);
    java.util.Map<String, Object> pack = (java.util.Map<String, Object>) root.get("pack");
    Assertions.assertEquals(97, pack.get("pack_format"));

    java.util.Map<String, Object> formats =
        (java.util.Map<String, Object>) pack.get("supported_formats");
    java.util.List<Integer> minFormat = (java.util.List<Integer>) formats.get("min_format");
    java.util.List<Integer> maxFormat = (java.util.List<Integer>) formats.get("max_format");

    Assertions.assertEquals(2, minFormat.size());
    Assertions.assertEquals(97, minFormat.get(0));
    Assertions.assertEquals(1, minFormat.get(1));

    Assertions.assertEquals(2, maxFormat.size());
    Assertions.assertEquals(97, maxFormat.get(0));
    Assertions.assertEquals(1, maxFormat.get(1));
  }
}
