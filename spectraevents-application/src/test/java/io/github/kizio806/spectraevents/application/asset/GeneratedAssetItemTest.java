package io.github.kizio806.spectraevents.application.asset;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class GeneratedAssetItemTest {

  @Test
  void generatedReferencesRoundTripToStableFloatSafeCustomModelData() {
    String reference = GeneratedAssetItem.reference("meteor", "root_1");

    GeneratedAssetItem.ParsedReference parsed = GeneratedAssetItem.parse(reference);

    Assertions.assertNotNull(parsed);
    Assertions.assertEquals("meteor", parsed.modelId());
    Assertions.assertEquals("root_1", parsed.nodeId());
    Assertions.assertEquals(
        GeneratedAssetItem.customModelData("meteor", "root_1"), parsed.customModelData());
    Assertions.assertTrue(parsed.customModelData() < 16_777_216);
  }

  @Test
  void rejectsMalformedGeneratedReference() {
    Assertions.assertThrows(
        IllegalArgumentException.class, () -> GeneratedAssetItem.parse("spectraevents:../unsafe"));
    Assertions.assertNull(GeneratedAssetItem.parse("minecraft:paper"));
  }
}
