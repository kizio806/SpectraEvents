package io.github.kizio806.spectraevents.application.model.compiler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.kizio806.spectraevents.application.model.animation.spec.AnimationSpec;
import io.github.kizio806.spectraevents.application.model.animation.spec.AnimationTrackSpec;
import io.github.kizio806.spectraevents.application.model.animation.spec.KeyframeSpec;
import io.github.kizio806.spectraevents.application.model.spec.InteractionSpec;
import io.github.kizio806.spectraevents.application.model.spec.ModelPartSpec;
import io.github.kizio806.spectraevents.application.model.spec.ModelSpec;
import io.github.kizio806.spectraevents.application.model.spec.TransformSpec;
import io.github.kizio806.spectraevents.core.visual.animation.AnimationId;
import io.github.kizio806.spectraevents.core.visual.model.ModelDefinition;
import io.github.kizio806.spectraevents.core.visual.model.ModelPartDefinition;
import io.github.kizio806.spectraevents.core.visual.model.ModelPartId;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ModelCompilerTest {

  private final ModelCompiler compiler = new ModelCompiler();

  @Test
  void testCompileValidMultiPartModel() {
    ModelSpec spec = new ModelSpec();
    spec.setId("test_meteor");

    ModelPartSpec coreSpec = new ModelPartSpec();
    coreSpec.setType("ITEM_DISPLAY");
    coreSpec.setItem("minecraft:magma_block");

    TransformSpec coreT = new TransformSpec();
    coreT.setTranslation(List.of(0.0f, 10.0f, 0.0f));
    coreSpec.setTransform(coreT);

    ModelPartSpec labelSpec = new ModelPartSpec();
    labelSpec.setType("TEXT_DISPLAY");
    labelSpec.setParent("core");
    labelSpec.setText("<red>METEOR");

    TransformSpec labelT = new TransformSpec();
    labelT.setTranslation(List.of(0.0f, 2.5f, 0.0f));
    labelSpec.setTransform(labelT);

    spec.setParts(Map.of("core", coreSpec, "label", labelSpec));

    InteractionSpec mainInteraction = new InteractionSpec();
    mainInteraction.setParent("core");
    mainInteraction.setWidth(3.0f);
    mainInteraction.setHeight(3.0f);
    spec.setInteractions(Map.of("main", mainInteraction));

    ModelDefinition definition = compiler.compile(spec);

    assertEquals("test_meteor", definition.id().value());
    assertEquals(2, definition.parts().size());
    assertEquals(1, definition.interactions().size());

    ModelPartDefinition compiledLabel = definition.findPart(ModelPartId.of("label")).orElseThrow();
    // Composed translation of label: parent (0,10,0) + local (0,2.5,0) = (0,12.5,0)
    assertEquals(0.0f, compiledLabel.composedTransform().translation().x());
    assertEquals(12.5f, compiledLabel.composedTransform().translation().y());
    assertEquals(0.0f, compiledLabel.composedTransform().translation().z());
  }

  @Test
  void testDetectCycle() {
    ModelSpec spec = new ModelSpec();
    spec.setId("cycle_model");

    ModelPartSpec partA = new ModelPartSpec();
    partA.setType("ITEM_DISPLAY");
    partA.setItem("minecraft:stone");
    partA.setParent("partB");

    ModelPartSpec partB = new ModelPartSpec();
    partB.setType("ITEM_DISPLAY");
    partB.setItem("minecraft:stone");
    partB.setParent("partA");

    spec.setParts(Map.of("partA", partA, "partB", partB));

    ModelCompilerException ex =
        assertThrows(ModelCompilerException.class, () -> compiler.compile(spec));
    assertTrue(ex.diagnostics().stream().anyMatch(d -> d.code().equals("HIERARCHY_CYCLE")));
  }

  @Test
  void testDetectMissingParent() {
    ModelSpec spec = new ModelSpec();
    spec.setId("missing_parent_model");

    ModelPartSpec partA = new ModelPartSpec();
    partA.setType("ITEM_DISPLAY");
    partA.setItem("minecraft:stone");
    partA.setParent("unknown_part");

    spec.setParts(Map.of("partA", partA));

    ModelCompilerException ex =
        assertThrows(ModelCompilerException.class, () -> compiler.compile(spec));
    assertTrue(ex.diagnostics().stream().anyMatch(d -> d.code().equals("UNKNOWN_PARENT_PART")));
  }

  @Test
  void testDetectInvalidPartType() {
    ModelSpec spec = new ModelSpec();
    spec.setId("invalid_type_model");

    ModelPartSpec partA = new ModelPartSpec();
    partA.setType("INVALID_TYPE");

    spec.setParts(Map.of("partA", partA));

    ModelCompilerException ex =
        assertThrows(ModelCompilerException.class, () -> compiler.compile(spec));
    assertTrue(ex.diagnostics().stream().anyMatch(d -> d.code().equals("UNKNOWN_PART_TYPE")));
  }

  @Test
  void compileWithAnimationsRetainsExecutablePlansAndModelDefinitions() {
    ModelSpec spec = new ModelSpec();
    spec.setId("animated_model");

    ModelPartSpec core = new ModelPartSpec();
    core.setType("ITEM_DISPLAY");
    core.setItem("minecraft:stone");
    spec.setParts(Map.of("core", core));

    KeyframeSpec start = new KeyframeSpec();
    start.setAt("0s");
    start.setValue(List.of(0.0f, 0.0f, 0.0f));
    KeyframeSpec end = new KeyframeSpec();
    end.setAt("1s");
    end.setValue(List.of(0.0f, 1.0f, 0.0f));

    AnimationTrackSpec track = new AnimationTrackSpec();
    track.setTranslation(List.of(start, end));
    AnimationSpec animation = new AnimationSpec();
    animation.setDuration("1s");
    animation.setTracks(Map.of("core", track));
    spec.setAnimations(Map.of("hover", animation));

    CompiledModel compiled = compiler.compileWithAnimations(spec);

    assertEquals(1, compiled.animations().size());
    assertTrue(compiled.definition().findAnimation(new AnimationId("hover")).isPresent());
    assertEquals(1, compiled.animations().get(new AnimationId("hover")).tracks().size());
  }

  @Test
  void testCompileBlockAndTextDisplayParts() {
    ModelSpec spec = new ModelSpec();
    spec.setId("complex_model");

    ModelPartSpec blockPart = new ModelPartSpec();
    blockPart.setType("BLOCK_DISPLAY");
    blockPart.setBlock("minecraft:obsidian");

    ModelPartSpec textPart = new ModelPartSpec();
    textPart.setType("TEXT_DISPLAY");
    textPart.setText("Level 100");

    spec.setParts(Map.of("block", blockPart, "text", textPart));

    ModelDefinition def = compiler.compile(spec);
    assertEquals(2, def.parts().size());
  }

  @Test
  void testDetectMissingAssetsForPartTypes() {
    ModelSpec specItem = new ModelSpec();
    specItem.setId("missing_item");
    ModelPartSpec partItem = new ModelPartSpec();
    partItem.setType("ITEM_DISPLAY"); // No item set
    specItem.setParts(Map.of("p1", partItem));

    ModelCompilerException ex1 =
        assertThrows(ModelCompilerException.class, () -> compiler.compile(specItem));
    assertTrue(
        ex1.diagnostics().stream().anyMatch(d -> d.code().equals("ITEM_DISPLAY_MISSING_ITEM")));

    ModelSpec specBlock = new ModelSpec();
    specBlock.setId("missing_block");
    ModelPartSpec partBlock = new ModelPartSpec();
    partBlock.setType("BLOCK_DISPLAY"); // No block set
    specBlock.setParts(Map.of("p1", partBlock));

    ModelCompilerException ex2 =
        assertThrows(ModelCompilerException.class, () -> compiler.compile(specBlock));
    assertTrue(
        ex2.diagnostics().stream().anyMatch(d -> d.code().equals("BLOCK_DISPLAY_MISSING_BLOCK")));

    ModelSpec specText = new ModelSpec();
    specText.setId("invalid_text_alignment");
    ModelPartSpec partText = new ModelPartSpec();
    partText.setType("TEXT_DISPLAY");
    partText.setTextAlignment("BOGUS_ALIGNMENT");
    specText.setParts(Map.of("p1", partText));

    ModelCompilerException ex3 =
        assertThrows(ModelCompilerException.class, () -> compiler.compile(specText));
    assertTrue(ex3.diagnostics().stream().anyMatch(d -> d.code().equals("UNKNOWN_TEXT_ALIGNMENT")));
  }

  @Test
  void testDetectInvalidInteractionParameters() {
    ModelSpec spec = new ModelSpec();
    spec.setId("bad_interactions");

    ModelPartSpec part = new ModelPartSpec();
    part.setType("ITEM_DISPLAY");
    part.setItem("minecraft:stone");
    spec.setParts(Map.of("core", part));

    InteractionSpec badSize = new InteractionSpec();
    badSize.setParent("core");
    badSize.setWidth(-1.0f);
    badSize.setHeight(0.0f);

    InteractionSpec missingParent = new InteractionSpec();
    missingParent.setParent("ghost_part");
    missingParent.setWidth(1.0f);
    missingParent.setHeight(1.0f);

    spec.setInteractions(Map.of("badSize", badSize, "missingParent", missingParent));

    ModelCompilerException ex =
        assertThrows(ModelCompilerException.class, () -> compiler.compile(spec));
    assertTrue(
        ex.diagnostics().stream().anyMatch(d -> d.code().equals("INVALID_INTERACTION_WIDTH")));
    assertTrue(
        ex.diagnostics().stream().anyMatch(d -> d.code().equals("INVALID_INTERACTION_HEIGHT")));
    assertTrue(
        ex.diagnostics().stream().anyMatch(d -> d.code().equals("UNKNOWN_INTERACTION_PARENT")));
  }

  @Test
  void testCompileRenderPropertiesAndRotations() {
    ModelSpec spec = new ModelSpec();
    spec.setId("render_props_model");

    ModelPartSpec part = new ModelPartSpec();
    part.setType("ITEM_DISPLAY");
    part.setItem("minecraft:diamond_sword");

    io.github.kizio806.spectraevents.application.model.spec.RenderPropertiesSpec renderSpec =
        new io.github.kizio806.spectraevents.application.model.spec.RenderPropertiesSpec();
    renderSpec.setBillboard("VERTICAL");
    renderSpec.setBrightnessBlock(12);
    renderSpec.setBrightnessSky(15);
    renderSpec.setShadowRadius(0.5f);
    renderSpec.setShadowStrength(0.8f);
    renderSpec.setViewRange(2.0f);
    renderSpec.setDisplayWidth(1.2f);
    renderSpec.setDisplayHeight(2.4f);
    renderSpec.setGlowColor("#00FF00");
    renderSpec.setInterpolationDelay(1);
    renderSpec.setInterpolationDuration(10);
    renderSpec.setTeleportDuration(2);
    part.setRender(renderSpec);

    TransformSpec transform = new TransformSpec();
    TransformSpec.RotationSpec rotEuler = new TransformSpec.RotationSpec();
    rotEuler.setEuler(List.of(15f, 30f, 45f));
    transform.setRotation(rotEuler);
    part.setTransform(transform);

    spec.setParts(Map.of("sword", part));

    ModelDefinition def = compiler.compile(spec);
    assertNotNull(def);
    var compiledPart = def.findPart(ModelPartId.of("sword")).orElseThrow();
    assertEquals(
        io.github.kizio806.spectraevents.core.visual.model.BillboardMode.VERTICAL,
        compiledPart.renderProperties().billboard());
    assertEquals(12, compiledPart.renderProperties().brightnessBlock());
    assertEquals(15, compiledPart.renderProperties().brightnessSky());

    // Test invalid billboard & invalid euler & invalid quaternion
    ModelSpec badSpec = new ModelSpec();
    badSpec.setId("bad_render");
    ModelPartSpec badPart = new ModelPartSpec();
    badPart.setType("ITEM_DISPLAY");
    badPart.setItem("minecraft:stone");

    io.github.kizio806.spectraevents.application.model.spec.RenderPropertiesSpec badRender =
        new io.github.kizio806.spectraevents.application.model.spec.RenderPropertiesSpec();
    badRender.setBillboard("BOGUS_BILLBOARD");
    badRender.setShadowRadius(-5.0f); // Invalid negative shadow radius
    badPart.setRender(badRender);

    TransformSpec badT = new TransformSpec();
    TransformSpec.RotationSpec badRot = new TransformSpec.RotationSpec();
    badRot.setEuler(List.of(10f)); // Needs 3
    badT.setRotation(badRot);
    badPart.setTransform(badT);

    badSpec.setParts(Map.of("p1", badPart));

    ModelCompilerException ex =
        assertThrows(ModelCompilerException.class, () -> compiler.compile(badSpec));
    assertTrue(ex.diagnostics().stream().anyMatch(d -> d.code().equals("UNKNOWN_BILLBOARD_MODE")));
    assertTrue(
        ex.diagnostics().stream().anyMatch(d -> d.code().equals("INVALID_RENDER_PROPERTIES")));
    assertTrue(ex.diagnostics().stream().anyMatch(d -> d.code().equals("INVALID_EULER_ROTATION")));
  }
}
