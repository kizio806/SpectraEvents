package io.github.kizio806.spectraevents.core.visual.asset;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class SpectraAssetFaceTest {
  @Test
  void preservesItsUvCoordinatesFromExternalMutation() {
    List<Double> sourceUv = new ArrayList<>(List.of(0.0, 1.0, 2.0, 3.0));
    SpectraAssetFace face = new SpectraAssetFace(sourceUv, "texture", 0);

    sourceUv.set(0, 99.0);

    assertEquals(List.of(0.0, 1.0, 2.0, 3.0), face.uv());
  }

  @Test
  void preservesEmbeddedTextureBytesFromExternalMutation() {
    byte[] sourceData = {1, 2, 3};
    SpectraAssetTexture texture = new SpectraAssetTexture("texture", sourceData, null);

    sourceData[0] = 9;
    byte[] returnedData = texture.data().orElseThrow();
    returnedData[1] = 9;

    assertEquals(1, texture.data().orElseThrow()[0]);
    assertEquals(2, texture.data().orElseThrow()[1]);
  }
}
