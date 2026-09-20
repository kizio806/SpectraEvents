package io.github.kizio806.spectraevents.application.model.compiler;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
}
