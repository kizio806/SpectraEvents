package io.github.kizio806.spectraevents.core.visual.asset;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.kizio806.spectraevents.core.visual.animation.AnimationDuration;
import io.github.kizio806.spectraevents.core.visual.animation.AnimationTime;
import io.github.kizio806.spectraevents.core.visual.animation.LoopMode;
import io.github.kizio806.spectraevents.core.visual.model.EulerRotation;
import io.github.kizio806.spectraevents.core.visual.model.Vector3;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class SpectraAssetDocumentTest {

  @Test
  void testSpectraAssetTexture() {
    byte[] testBytes = new byte[] {1, 2, 3, 4};
    SpectraAssetTexture tex = new SpectraAssetTexture("diffuse", testBytes, "textures/diffuse.png");
    assertEquals("diffuse", tex.name());
    assertEquals("textures/diffuse.png", tex.sourcePath());
    assertTrue(tex.data().isPresent());
    assertArrayEquals(testBytes, tex.data().get());

    // Defensive copy check
    testBytes[0] = 99;
    assertEquals(1, tex.data().get()[0]);

    // Path only (no data)
    SpectraAssetTexture pathOnly = new SpectraAssetTexture("tex2", null, "textures/tex2.png");
    assertFalse(pathOnly.data().isPresent());
    assertEquals("tex2", pathOnly.name());

    // Data only (no path)
    SpectraAssetTexture dataOnly = new SpectraAssetTexture("tex3", new byte[] {5}, null);
    assertTrue(dataOnly.data().isPresent());
    assertNull(dataOnly.sourcePath());

    // Validation
    assertThrows(
        NullPointerException.class, () -> new SpectraAssetTexture(null, new byte[] {1}, "path"));
    assertThrows(
        IllegalArgumentException.class, () -> new SpectraAssetTexture("", new byte[] {1}, "path"));
    assertThrows(IllegalArgumentException.class, () -> new SpectraAssetTexture("tex", null, null));
    assertThrows(IllegalArgumentException.class, () -> new SpectraAssetTexture("tex", null, "   "));
  }

  @Test
  void testSpectraAssetFace() {
    List<Double> uv = List.of(0.0, 0.0, 16.0, 16.0);
    SpectraAssetFace face = new SpectraAssetFace(uv, "diffuse", 90);
    assertEquals(uv, face.uv());
    assertEquals("diffuse", face.textureRef());
    assertEquals(90, face.rotation());

    // Validation
    assertThrows(NullPointerException.class, () -> new SpectraAssetFace(null, "diffuse", 0));
    assertThrows(
        IllegalArgumentException.class,
        () -> new SpectraAssetFace(List.of(0.0, 1.0), "diffuse", 0));
    assertThrows(IllegalArgumentException.class, () -> new SpectraAssetFace(uv, null, 0));
    assertThrows(IllegalArgumentException.class, () -> new SpectraAssetFace(uv, "   ", 0));
  }

  @Test
  void testSpectraAssetGeometry() {
    SpectraAssetFace face = new SpectraAssetFace(List.of(0.0, 0.0, 16.0, 16.0), "diffuse", 0);
    SpectraAssetGeometry geom =
        new SpectraAssetGeometry(
            Vector3.ZERO,
            Vector3.ONE,
            Vector3.ZERO,
            EulerRotation.ZERO,
            0.1,
            Map.of("north", face));

    assertEquals(Vector3.ZERO, geom.from());
    assertEquals(Vector3.ONE, geom.to());
    assertEquals(Vector3.ZERO, geom.origin());
    assertEquals(EulerRotation.ZERO, geom.rotation());
    assertEquals(0.1, geom.inflate());
    assertEquals(1, geom.faces().size());

    assertThrows(
        NullPointerException.class,
        () ->
            new SpectraAssetGeometry(
                null, Vector3.ONE, Vector3.ZERO, EulerRotation.ZERO, 0, Map.of()));
    assertThrows(
        NullPointerException.class,
        () ->
            new SpectraAssetGeometry(
                Vector3.ZERO, null, Vector3.ZERO, EulerRotation.ZERO, 0, Map.of()));
    assertThrows(
        NullPointerException.class,
        () ->
            new SpectraAssetGeometry(
                Vector3.ZERO, Vector3.ONE, null, EulerRotation.ZERO, 0, Map.of()));
    assertThrows(
        NullPointerException.class,
        () -> new SpectraAssetGeometry(Vector3.ZERO, Vector3.ONE, Vector3.ZERO, null, 0, Map.of()));
    assertThrows(
        NullPointerException.class,
        () ->
            new SpectraAssetGeometry(
                Vector3.ZERO, Vector3.ONE, Vector3.ZERO, EulerRotation.ZERO, 0, null));
  }

  @Test
  void testSpectraAssetNode() {
    SpectraAssetNode node =
        new SpectraAssetNode(
            "bone1",
            Vector3.ZERO,
            Vector3.ZERO,
            EulerRotation.ZERO,
            Vector3.ONE,
            List.of(),
            List.of());

    assertEquals("bone1", node.nodeId());
    assertEquals(Vector3.ZERO, node.pivot());
    assertEquals(0, node.cubes().size());
    assertEquals(0, node.children().size());

    assertThrows(
        IllegalArgumentException.class,
        () ->
            new SpectraAssetNode(
                "",
                Vector3.ZERO,
                Vector3.ZERO,
                EulerRotation.ZERO,
                Vector3.ONE,
                List.of(),
                List.of()));
    assertThrows(
        NullPointerException.class,
        () ->
            new SpectraAssetNode(
                null,
                Vector3.ZERO,
                Vector3.ZERO,
                EulerRotation.ZERO,
                Vector3.ONE,
                List.of(),
                List.of()));
  }

  @Test
  void testSpectraAssetAnimation() {
    SpectraAssetAnimation anim =
        new SpectraAssetAnimation(
            "walk",
            AnimationDuration.of(AnimationTime.fromSeconds(1.0)),
            LoopMode.LOOP,
            Map.of(),
            Map.of(),
            Map.of(),
            List.of());

    assertEquals("walk", anim.animationId());
    assertEquals(LoopMode.LOOP, anim.loopMode());
    assertEquals(0, anim.translationTracks().size());

    assertThrows(
        IllegalArgumentException.class,
        () ->
            new SpectraAssetAnimation(
                "",
                AnimationDuration.of(AnimationTime.fromSeconds(1.0)),
                LoopMode.LOOP,
                Map.of(),
                Map.of(),
                Map.of(),
                List.of()));
    assertThrows(
        NullPointerException.class,
        () ->
            new SpectraAssetAnimation(
                null,
                AnimationDuration.of(AnimationTime.fromSeconds(1.0)),
                LoopMode.LOOP,
                Map.of(),
                Map.of(),
                Map.of(),
                List.of()));
  }

  @Test
  void testSpectraAssetManifest() {
    SpectraAssetManifest manifest =
        new SpectraAssetManifest(
            1,
            "SpectraEvents",
            "1.0.0",
            "boss",
            "bbmodel",
            "4.10",
            "4.10.0",
            Map.of("model.bbmodel", "abcdef123456"));

    assertEquals(1, manifest.schemaVersion());
    assertEquals("SpectraEvents", manifest.generator());
    assertEquals("boss", manifest.modelId());
    assertEquals(1, manifest.checksums().size());

    assertThrows(
        NullPointerException.class,
        () ->
            new SpectraAssetManifest(
                1, null, "1.0.0", "boss", "bbmodel", "4.10", "4.10.0", Map.of()));
  }

  @Test
  void testSpectraAssetDocument() {
    SpectraAssetDocument doc = new SpectraAssetDocument(1, "boss", Map.of(), List.of(), Map.of());

    assertEquals(1, doc.schemaVersion());
    assertEquals("boss", doc.modelId());
    assertEquals(0, doc.textures().size());
    assertEquals(0, doc.nodes().size());
    assertEquals(0, doc.animations().size());

    assertThrows(
        IllegalArgumentException.class,
        () -> new SpectraAssetDocument(1, "", Map.of(), List.of(), Map.of()));
    assertThrows(
        NullPointerException.class,
        () -> new SpectraAssetDocument(1, null, Map.of(), List.of(), Map.of()));
    assertThrows(
        NullPointerException.class,
        () -> new SpectraAssetDocument(1, "boss", null, List.of(), Map.of()));
  }
}
