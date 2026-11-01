package io.github.kizio806.spectraevents.application.model.spec;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ModelSpecTest {

  @Test
  void testRenderPropertiesSpecGettersSetters() {
    RenderPropertiesSpec spec = new RenderPropertiesSpec();
    spec.setBillboard("CENTER");
    spec.setBrightnessBlock(15);
    spec.setBrightnessSky(15);
    spec.setShadowRadius(0.5f);
    spec.setShadowStrength(0.8f);
    spec.setViewRange(2.0f);
    spec.setDisplayWidth(1.5f);
    spec.setDisplayHeight(2.5f);
    spec.setGlowColor("#FF00FF");
    spec.setInterpolationDelay(1);
    spec.setInterpolationDuration(5);
    spec.setTeleportDuration(2);

    assertEquals("CENTER", spec.getBillboard());
    assertEquals(15, spec.getBrightnessBlock());
    assertEquals(15, spec.getBrightnessSky());
    assertEquals(0.5f, spec.getShadowRadius());
    assertEquals(0.8f, spec.getShadowStrength());
    assertEquals(2.0f, spec.getViewRange());
    assertEquals(1.5f, spec.getDisplayWidth());
    assertEquals(2.5f, spec.getDisplayHeight());
    assertEquals("#FF00FF", spec.getGlowColor());
    assertEquals(1, spec.getInterpolationDelay());
    assertEquals(5, spec.getInterpolationDuration());
    assertEquals(2, spec.getTeleportDuration());
  }

  @Test
  void testTransformSpecGettersSetters() {
    TransformSpec spec = new TransformSpec();
    spec.setTranslation(List.of(1.0f, 2.0f, 3.0f));
    spec.setScale(List.of(2.0f, 2.0f, 2.0f));
    spec.setPivot(List.of(0.5f, 0.5f, 0.5f));

    TransformSpec.RotationSpec rot = new TransformSpec.RotationSpec();
    rot.setEuler(List.of(0f, 90f, 0f));
    rot.setQuaternion(List.of(0f, 0f, 0f, 1f));
    spec.setRotation(rot);

    assertEquals(List.of(1.0f, 2.0f, 3.0f), spec.getTranslation());
    assertEquals(List.of(2.0f, 2.0f, 2.0f), spec.getScale());
    assertEquals(List.of(0.5f, 0.5f, 0.5f), spec.getPivot());
    assertNotNull(spec.getRotation());
    assertEquals(List.of(0f, 90f, 0f), spec.getRotation().getEuler());
    assertEquals(List.of(0f, 0f, 0f, 1f), spec.getRotation().getQuaternion());
  }

  @Test
  void testInteractionSpecGettersSetters() {
    InteractionSpec spec = new InteractionSpec();
    spec.setParent("core");
    spec.setOffset(List.of(0f, 1f, 0f));
    spec.setWidth(2.0f);
    spec.setHeight(3.0f);
    spec.setResponsive(true);

    assertEquals("core", spec.getParent());
    assertEquals(List.of(0f, 1f, 0f), spec.getOffset());
    assertEquals(2.0f, spec.getWidth());
    assertEquals(3.0f, spec.getHeight());
    assertTrue(spec.getResponsive());
  }

  @Test
  void testModelPartAndModelSpec() {
    ModelPartSpec part = new ModelPartSpec();
    part.setType("ITEM_DISPLAY");
    part.setItem("minecraft:diamond");
    part.setBlock("minecraft:stone");
    part.setText("label");
    part.setTextAlignment("CENTER");
    part.setTransformMode("THIRDPERSON_RIGHTHAND");
    part.setParent("root");

    assertEquals("ITEM_DISPLAY", part.getType());
    assertEquals("minecraft:diamond", part.getItem());
    assertEquals("minecraft:stone", part.getBlock());
    assertEquals("label", part.getText());
    assertEquals("CENTER", part.getTextAlignment());
    assertEquals("THIRDPERSON_RIGHTHAND", part.getTransformMode());
    assertEquals("root", part.getParent());

    ModelSpec model = new ModelSpec();
    model.setId("boss");
    model.setParts(Map.of("p1", part));
    assertEquals("boss", model.getId());
    assertEquals(1, model.getParts().size());
  }
}
