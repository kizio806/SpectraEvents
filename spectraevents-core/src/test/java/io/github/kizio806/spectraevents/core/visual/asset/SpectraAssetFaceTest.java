package io.github.kizio806.spectraevents.core.visual.asset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class SpectraAssetFaceTest {

  @Test
  void preservesItsUvCoordinatesFromExternalMutation() {
    List<Double> sourceUv = new ArrayList<>(List.of(0.0, 1.0, 2.0, 3.0));
    SpectraAssetFace face = new SpectraAssetFace(sourceUv, "texture", 90);

    mutateList(sourceUv, 0, 99.0);

    assertEquals(List.of(0.0, 1.0, 2.0, 3.0), face.uv());
    assertEquals("texture", face.textureRef());
    assertEquals(90, face.rotation());
  }

  @Test
  void preventsMutationOfReturnedUvList() {
    SpectraAssetFace face = new SpectraAssetFace(List.of(0.0, 1.0, 2.0, 3.0), "texture", 0);
    List<Double> returnedUv = face.uv();

    assertThrows(UnsupportedOperationException.class, () -> returnedUv.add(4.0));
    assertThrows(UnsupportedOperationException.class, () -> returnedUv.set(0, 42.0));
  }

  @Test
  void rejectsEmptyUvList() {
    IllegalArgumentException exception =
        assertThrows(
            IllegalArgumentException.class, () -> new SpectraAssetFace(List.of(), "texture", 0));
    assertEquals("UV must have exactly 4 elements", exception.getMessage());
  }

  @Test
  void rejectsUvListWithIncorrectSize() {
    assertThrows(
        IllegalArgumentException.class,
        () -> new SpectraAssetFace(List.of(1.0, 2.0, 3.0), "texture", 0));
    assertThrows(
        IllegalArgumentException.class,
        () -> new SpectraAssetFace(List.of(1.0, 2.0, 3.0, 4.0, 5.0), "texture", 0));
  }

  @Test
  void rejectsNullUvList() {
    assertThrows(NullPointerException.class, () -> new SpectraAssetFace(null, "texture", 0));
  }

  @Test
  void rejectsNullOrBlankTextureRef() {
    assertThrows(
        IllegalArgumentException.class,
        () -> new SpectraAssetFace(List.of(0.0, 1.0, 2.0, 3.0), null, 0));
    assertThrows(
        IllegalArgumentException.class,
        () -> new SpectraAssetFace(List.of(0.0, 1.0, 2.0, 3.0), "", 0));
    assertThrows(
        IllegalArgumentException.class,
        () -> new SpectraAssetFace(List.of(0.0, 1.0, 2.0, 3.0), "   ", 0));
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

  private static <T> void mutateList(List<T> list, int index, T value) {
    list.set(index, value);
  }
}
