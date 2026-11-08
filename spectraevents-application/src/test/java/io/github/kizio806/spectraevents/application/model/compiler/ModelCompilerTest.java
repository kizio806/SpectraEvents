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
import io.github.kizio806.spectraevents.application.model.spec.RenderPropertiesSpec;
import io.github.kizio806.spectraevents.application.model.spec.TransformSpec;
import io.github.kizio806.spectraevents.core.visual.animation.AnimationId;
import io.github.kizio806.spectraevents.core.visual.model.BillboardMode;
import io.github.kizio806.spectraevents.core.visual.model.ModelDefinition;
import io.github.kizio806.spectraevents.core.visual.model.ModelPartDefinition;
import io.github.kizio806.spectraevents.core.visual.model.ModelPartId;
import java.util.HashMap;
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

    RenderPropertiesSpec renderSpec = new RenderPropertiesSpec();
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
    assertEquals(BillboardMode.VERTICAL, compiledPart.renderProperties().billboard());
    assertEquals(12, compiledPart.renderProperties().brightnessBlock());
    assertEquals(15, compiledPart.renderProperties().brightnessSky());

    // Test invalid billboard & invalid euler & invalid quaternion
    ModelSpec badSpec = new ModelSpec();
    badSpec.setId("bad_render");
    ModelPartSpec badPart = new ModelPartSpec();
    badPart.setType("ITEM_DISPLAY");
    badPart.setItem("minecraft:stone");

    RenderPropertiesSpec badRender = new RenderPropertiesSpec();
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

  @Test
  void testCompileModelWithBlankIdAndNoParts() {
    // 1. Null and blank id
    ModelSpec specBlankId = new ModelSpec();
    specBlankId.setId("   ");
    ModelPartSpec part = new ModelPartSpec();
    part.setType("ITEM_DISPLAY");
    part.setItem("minecraft:stone");
    specBlankId.setParts(Map.of("core", part));

    ModelCompilerException exId =
        assertThrows(ModelCompilerException.class, () -> compiler.compile(specBlankId));
    assertTrue(exId.diagnostics().stream().anyMatch(d -> d.code().equals("MODEL_ID_BLANK")));

    // 2. Null and empty parts
    ModelSpec specNoParts = new ModelSpec();
    specNoParts.setId("no_parts");
    specNoParts.setParts(null);

    ModelCompilerException exNoParts =
        assertThrows(ModelCompilerException.class, () -> compiler.compile(specNoParts));
    assertTrue(exNoParts.diagnostics().stream().anyMatch(d -> d.code().equals("MODEL_NO_PARTS")));

    ModelSpec specEmptyParts = new ModelSpec();
    specEmptyParts.setId("empty_parts");
    specEmptyParts.setParts(Map.of());

    ModelCompilerException exEmptyParts =
        assertThrows(ModelCompilerException.class, () -> compiler.compile(specEmptyParts));
    assertTrue(
        exEmptyParts.diagnostics().stream().anyMatch(d -> d.code().equals("MODEL_NO_PARTS")));
  }

  @Test
  void testCompileModelWithBlankPartAndInteractionAndAnimationIds() {
    ModelPartSpec part = new ModelPartSpec();
    part.setType("ITEM_DISPLAY");
    part.setItem("minecraft:stone");

    // 1. Blank part ID
    ModelSpec specPart = new ModelSpec();
    specPart.setId("blank_part");
    java.util.HashMap<String, ModelPartSpec> parts = new java.util.HashMap<>();
    parts.put("", part);
    parts.put("core", part);
    specPart.setParts(parts);

    ModelCompilerException exPart =
        assertThrows(ModelCompilerException.class, () -> compiler.compile(specPart));
    assertTrue(exPart.diagnostics().stream().anyMatch(d -> d.code().equals("PART_ID_BLANK")));

    // 2. Blank interaction ID
    ModelSpec specInteraction = new ModelSpec();
    specInteraction.setId("blank_interaction");
    specInteraction.setParts(Map.of("core", part));
    java.util.HashMap<String, InteractionSpec> interactions = new java.util.HashMap<>();
    InteractionSpec iSpec = new InteractionSpec();
    iSpec.setParent("core");
    interactions.put("", iSpec);
    specInteraction.setInteractions(interactions);

    ModelCompilerException exInt =
        assertThrows(ModelCompilerException.class, () -> compiler.compile(specInteraction));
    assertTrue(exInt.diagnostics().stream().anyMatch(d -> d.code().equals("INTERACTION_ID_BLANK")));

    // 3. Blank animation ID and null spec
    ModelSpec specAnim = new ModelSpec();
    specAnim.setId("blank_animation");
    specAnim.setParts(Map.of("core", part));
    java.util.HashMap<String, AnimationSpec> animations = new java.util.HashMap<>();
    animations.put("", new AnimationSpec());
    animations.put("null_spec", null);
    specAnim.setAnimations(animations);

    ModelCompilerException exAnim =
        assertThrows(ModelCompilerException.class, () -> compiler.compile(specAnim));
    assertTrue(exAnim.diagnostics().stream().anyMatch(d -> d.code().equals("ANIMATION_ID_BLANK")));
  }

  @Test
  void testCompilePartTypeBlankAndTransformModes() {
    // 1. Blank part type
    ModelSpec specBlankType = new ModelSpec();
    specBlankType.setId("blank_type");
    ModelPartSpec part = new ModelPartSpec();
    part.setType("   ");
    specBlankType.setParts(Map.of("p1", part));

    ModelCompilerException exType =
        assertThrows(ModelCompilerException.class, () -> compiler.compile(specBlankType));
    assertTrue(exType.diagnostics().stream().anyMatch(d -> d.code().equals("PART_TYPE_BLANK")));

    // 2. Valid and invalid DisplayTransformMode
    ModelSpec specModes = new ModelSpec();
    specModes.setId("modes");

    ModelPartSpec pValid = new ModelPartSpec();
    pValid.setType("ITEM_DISPLAY");
    pValid.setItem("minecraft:diamond");
    pValid.setTransformMode("HEAD");

    ModelPartSpec pInvalid = new ModelPartSpec();
    pInvalid.setType("ITEM_DISPLAY");
    pInvalid.setItem("minecraft:gold_ingot");
    pInvalid.setTransformMode("BOGUS_MODE");

    specModes.setParts(Map.of("valid", pValid, "invalid", pInvalid));

    ModelCompilerException exMode =
        assertThrows(ModelCompilerException.class, () -> compiler.compile(specModes));
    assertTrue(
        exMode.diagnostics().stream().anyMatch(d -> d.code().equals("UNKNOWN_TRANSFORM_MODE")));

    // 3. Transform with Quaternion rotation and empty rotation
    ModelSpec specQuat = new ModelSpec();
    specQuat.setId("quat_rot_model");
    ModelPartSpec pQuat = new ModelPartSpec();
    pQuat.setType("ITEM_DISPLAY");
    pQuat.setItem("minecraft:iron_sword");

    TransformSpec tQuat = new TransformSpec();
    TransformSpec.RotationSpec rQuat = new TransformSpec.RotationSpec();
    rQuat.setQuaternion(List.of(0.0f, 0.0f, 0.0f, 1.0f));
    tQuat.setRotation(rQuat);
    pQuat.setTransform(tQuat);

    ModelPartSpec pEmptyRot = new ModelPartSpec();
    pEmptyRot.setType("ITEM_DISPLAY");
    pEmptyRot.setItem("minecraft:bow");
    TransformSpec tEmpty = new TransformSpec();
    tEmpty.setRotation(new TransformSpec.RotationSpec());
    pEmptyRot.setTransform(tEmpty);

    specQuat.setParts(Map.of("quat", pQuat, "empty_rot", pEmptyRot));
    ModelDefinition defQuat = compiler.compile(specQuat);
    assertEquals(2, defQuat.parts().size());
  }

  @Test
  void testCompileTextDisplayProperties() {
    ModelSpec spec = new ModelSpec();
    spec.setId("text_model");

    ModelPartSpec textPart = new ModelPartSpec();
    textPart.setType("TEXT_DISPLAY");
    textPart.setText("Hello World");
    textPart.setTextAlignment("LEFT");
    textPart.setLineWidth(150);
    textPart.setBackgroundColor(0xFF0000);
    textPart.setTextOpacity(128);
    textPart.setShadow(false);
    textPart.setSeeThrough(true);

    spec.setParts(Map.of("title", textPart));

    ModelDefinition def = compiler.compile(spec);
    assertEquals(1, def.parts().size());
    var part = def.findPart(ModelPartId.of("title")).orElseThrow();
    assertTrue(
        part.visualAsset()
            instanceof io.github.kizio806.spectraevents.core.visual.model.TextAssetRef);
    var textRef =
        (io.github.kizio806.spectraevents.core.visual.model.TextAssetRef) part.visualAsset();
    assertEquals("Hello World", textRef.text());
    assertEquals(
        io.github.kizio806.spectraevents.core.visual.model.TextAlignment.LEFT, textRef.alignment());
    assertEquals(150, textRef.lineWidth());
    assertEquals(0xFF0000, textRef.backgroundColor());
    assertEquals(128, textRef.textOpacity());
    assertEquals(false, textRef.shadow());
    assertEquals(true, textRef.seeThrough());
  }

  @Test
  void testCompileModelEdgeCasesAndGuardrails() {
    // 1. Too many parts (> MAX_PARTS_LIMIT)
    ModelSpec specTooManyParts = new ModelSpec();
    specTooManyParts.setId("too_many_parts");
    Map<String, ModelPartSpec> parts = new HashMap<>();
    for (int i = 0; i <= ModelDefinition.MAX_PARTS_LIMIT; i++) {
      ModelPartSpec p = new ModelPartSpec();
      p.setType("ITEM_DISPLAY");
      p.setItem("minecraft:stone");
      parts.put("part_" + i, p);
    }
    specTooManyParts.setParts(parts);
    ModelCompilerException exParts =
        assertThrows(ModelCompilerException.class, () -> compiler.compile(specTooManyParts));
    assertTrue(
        exParts.diagnostics().stream().anyMatch(d -> "MODEL_TOO_MANY_PARTS".equals(d.code())));

    // 2. Too many interactions (> MAX_INTERACTIONS_LIMIT)
    ModelSpec specTooManyInteractions = new ModelSpec();
    specTooManyInteractions.setId("too_many_interactions");
    ModelPartSpec basePart = new ModelPartSpec();
    basePart.setType("ITEM_DISPLAY");
    basePart.setItem("minecraft:stone");
    specTooManyInteractions.setParts(Map.of("base", basePart));
    Map<String, io.github.kizio806.spectraevents.application.model.spec.InteractionSpec>
        interactions = new HashMap<>();
    for (int i = 0; i <= ModelDefinition.MAX_INTERACTIONS_LIMIT; i++) {
      io.github.kizio806.spectraevents.application.model.spec.InteractionSpec is =
          new io.github.kizio806.spectraevents.application.model.spec.InteractionSpec();
      is.setWidth(1.0f);
      is.setHeight(1.0f);
      interactions.put("inter_" + i, is);
    }
    specTooManyInteractions.setInteractions(interactions);
    ModelCompilerException exInter =
        assertThrows(ModelCompilerException.class, () -> compiler.compile(specTooManyInteractions));
    assertTrue(
        exInter.diagnostics().stream().anyMatch(d -> "TOO_MANY_INTERACTIONS".equals(d.code())));

    // 3. Hierarchy too deep (> MAX_HIERARCHY_DEPTH)
    ModelSpec specDeep = new ModelSpec();
    specDeep.setId("deep_model");
    Map<String, ModelPartSpec> deepParts = new HashMap<>();
    for (int i = 0; i <= 35; i++) {
      ModelPartSpec p = new ModelPartSpec();
      p.setType("ITEM_DISPLAY");
      p.setItem("minecraft:stone");
      if (i > 0) {
        p.setParent("node_" + (i - 1));
      }
      deepParts.put("node_" + i, p);
    }
    specDeep.setParts(deepParts);
    ModelCompilerException exDeep =
        assertThrows(ModelCompilerException.class, () -> compiler.compile(specDeep));
    assertTrue(exDeep.diagnostics().stream().anyMatch(d -> "HIERARCHY_TOO_DEEP".equals(d.code())));

    // 4. Invalid transform (zero scale throws in ModelTransform)
    ModelSpec specZeroScale = new ModelSpec();
    specZeroScale.setId("zero_scale");
    ModelPartSpec pZero = new ModelPartSpec();
    pZero.setType("ITEM_DISPLAY");
    pZero.setItem("minecraft:stone");
    TransformSpec tZero = new TransformSpec();
    tZero.setScale(List.of(0.0f, 1.0f, 1.0f));
    pZero.setTransform(tZero);
    specZeroScale.setParts(Map.of("p_zero", pZero));
    ModelCompilerException exZero =
        assertThrows(ModelCompilerException.class, () -> compiler.compile(specZeroScale));
    assertTrue(exZero.diagnostics().stream().anyMatch(d -> "INVALID_TRANSFORM".equals(d.code())));

    // 5. Embedded animation compiler exception propagated
    ModelSpec specBadAnim = new ModelSpec();
    specBadAnim.setId("bad_anim_model");
    ModelPartSpec pValid = new ModelPartSpec();
    pValid.setType("ITEM_DISPLAY");
    pValid.setItem("minecraft:stone");
    specBadAnim.setParts(Map.of("base", pValid));
    io.github.kizio806.spectraevents.application.model.animation.spec.AnimationSpec badAnim =
        new io.github.kizio806.spectraevents.application.model.animation.spec.AnimationSpec();
    badAnim.setDuration("0s"); // invalid duration throws AnimationCompilerException
    specBadAnim.setAnimations(Map.of("anim1", badAnim));
    ModelCompilerException exAnim =
        assertThrows(ModelCompilerException.class, () -> compiler.compile(specBadAnim));
    assertTrue(
        exAnim.diagnostics().stream()
            .anyMatch(d -> "ANIMATION_DURATION_NOT_POSITIVE".equals(d.code())));

    // 6. Interaction defaults and unresponsive flag
    ModelSpec specInteractionDefaults = new ModelSpec();
    specInteractionDefaults.setId("inter_defaults");
    ModelPartSpec basePart2 = new ModelPartSpec();
    basePart2.setType("ITEM_DISPLAY");
    basePart2.setItem("minecraft:stone");
    specInteractionDefaults.setParts(Map.of("base", basePart2));
    InteractionSpec isDefault = new InteractionSpec();
    InteractionSpec isUnresponsive = new InteractionSpec();
    isUnresponsive.setWidth(2.0f);
    isUnresponsive.setHeight(2.0f);
    isUnresponsive.setResponsive(false);
    specInteractionDefaults.setInteractions(Map.of("i_def", isDefault, "i_unresp", isUnresponsive));
    ModelDefinition defDefaults = compiler.compile(specInteractionDefaults);
    assertEquals(2, defDefaults.interactions().size());
  }
}
