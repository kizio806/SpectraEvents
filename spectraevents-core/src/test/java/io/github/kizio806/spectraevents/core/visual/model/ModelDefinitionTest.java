package io.github.kizio806.spectraevents.core.visual.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.kizio806.spectraevents.core.visual.animation.AnimationDefinition;
import io.github.kizio806.spectraevents.core.visual.animation.AnimationId;
import io.github.kizio806.spectraevents.core.visual.animation.LoopMode;
import io.github.kizio806.spectraevents.core.visual.animation.RecoveryPolicy;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ModelDefinitionTest {

  @Test
  void testValidModelDefinition() {
    ModelPartDefinition part =
        new ModelPartDefinition(
            new ModelPartId("head"),
            null,
            ModelPartType.ITEM_DISPLAY,
            ModelTransform.IDENTITY,
            ModelTransform.IDENTITY,
            ModelRenderProperties.DEFAULT,
            new ItemAssetRef("minecraft:diamond_helmet", DisplayTransformMode.HEAD));

    InteractionDefinition interaction =
        new InteractionDefinition(
            new InteractionId("hitbox"),
            new ModelPartId("head"),
            Vector3.ZERO,
            Vector3.ZERO,
            1.5f,
            2.0f,
            true);

    AnimationDefinition anim =
        new AnimationDefinition(
            new AnimationId("idle"),
            io.github.kizio806.spectraevents.core.visual.animation.AnimationDuration.of(
                io.github.kizio806.spectraevents.core.visual.animation.AnimationTime.fromSeconds(
                    1.0)),
            LoopMode.LOOP,
            1,
            Map.of(),
            List.of(),
            RecoveryPolicy.STOP);

    ModelDefinition model =
        new ModelDefinition(
            new ModelId("custom_boss"),
            List.of(part),
            List.of(interaction),
            Map.of(anim.id(), anim));

    assertEquals("custom_boss", model.id().value());
    assertEquals(1, model.parts().size());
    assertEquals(1, model.interactions().size());
    assertEquals(1, model.animations().size());

    assertTrue(model.findPart(new ModelPartId("head")).isPresent());
    assertFalse(model.findPart(new ModelPartId("body")).isPresent());
    assertTrue(model.findInteraction(new InteractionId("hitbox")).isPresent());
    assertFalse(model.findInteraction(new InteractionId("other")).isPresent());
    assertTrue(model.findAnimation(new AnimationId("idle")).isPresent());
    assertFalse(model.findAnimation(new AnimationId("walk")).isPresent());
  }

  @Test
  void testModelDefinitionValidation() {
    ModelPartDefinition part =
        new ModelPartDefinition(
            new ModelPartId("head"),
            null,
            ModelPartType.ITEM_DISPLAY,
            ModelTransform.IDENTITY,
            ModelTransform.IDENTITY,
            ModelRenderProperties.DEFAULT,
            new ItemAssetRef("minecraft:diamond_helmet", DisplayTransformMode.HEAD));

    // Empty parts
    assertThrows(
        IllegalArgumentException.class,
        () -> new ModelDefinition(new ModelId("test"), List.of(), List.of()));

    // Null checks
    assertThrows(
        NullPointerException.class, () -> new ModelDefinition(null, List.of(part), List.of()));
    assertThrows(
        NullPointerException.class,
        () -> new ModelDefinition(new ModelId("test"), null, List.of()));
    assertThrows(
        NullPointerException.class,
        () -> new ModelDefinition(new ModelId("test"), List.of(part), null));

    // Duplicate part IDs
    assertThrows(
        IllegalArgumentException.class,
        () -> new ModelDefinition(new ModelId("test"), List.of(part, part), List.of()));

    // Duplicate interaction IDs
    InteractionDefinition interaction =
        new InteractionDefinition(
            new InteractionId("box"),
            new ModelPartId("head"),
            Vector3.ZERO,
            Vector3.ZERO,
            1.0f,
            1.0f,
            false);
    assertThrows(
        IllegalArgumentException.class,
        () ->
            new ModelDefinition(
                new ModelId("test"), List.of(part), List.of(interaction, interaction)));
  }

  @Test
  void testModelPartDefinitionAssetMatching() {
    ModelRenderProperties props = ModelRenderProperties.DEFAULT;

    // ITEM_DISPLAY mismatch
    assertThrows(
        IllegalArgumentException.class,
        () ->
            new ModelPartDefinition(
                new ModelPartId("part"),
                null,
                ModelPartType.ITEM_DISPLAY,
                ModelTransform.IDENTITY,
                ModelTransform.IDENTITY,
                props,
                new BlockAssetRef("minecraft:stone")));

    // BLOCK_DISPLAY mismatch
    assertThrows(
        IllegalArgumentException.class,
        () ->
            new ModelPartDefinition(
                new ModelPartId("part"),
                null,
                ModelPartType.BLOCK_DISPLAY,
                ModelTransform.IDENTITY,
                ModelTransform.IDENTITY,
                props,
                new ItemAssetRef("minecraft:stone", DisplayTransformMode.NONE)));

    // TEXT_DISPLAY mismatch
    assertThrows(
        IllegalArgumentException.class,
        () ->
            new ModelPartDefinition(
                new ModelPartId("part"),
                null,
                ModelPartType.TEXT_DISPLAY,
                ModelTransform.IDENTITY,
                ModelTransform.IDENTITY,
                props,
                new ItemAssetRef("minecraft:stone", DisplayTransformMode.NONE)));

    // Correct BLOCK_DISPLAY and TEXT_DISPLAY
    ModelPartDefinition blockPart =
        new ModelPartDefinition(
            new ModelPartId("block"),
            null,
            ModelPartType.BLOCK_DISPLAY,
            ModelTransform.IDENTITY,
            ModelTransform.IDENTITY,
            props,
            new BlockAssetRef("minecraft:stone"));
    assertEquals(ModelPartType.BLOCK_DISPLAY, blockPart.type());

    ModelPartDefinition textPart =
        new ModelPartDefinition(
            new ModelPartId("text"),
            null,
            ModelPartType.TEXT_DISPLAY,
            ModelTransform.IDENTITY,
            ModelTransform.IDENTITY,
            props,
            TextAssetRef.of("hello"));
    assertEquals(ModelPartType.TEXT_DISPLAY, textPart.type());
  }

  @Test
  void testInteractionDefinitionValidation() {
    InteractionId id = new InteractionId("i1");
    ModelPartId partId = new ModelPartId("p1");

    assertThrows(
        IllegalArgumentException.class,
        () ->
            new InteractionDefinition(id, partId, Vector3.ZERO, Vector3.ZERO, -1.0f, 1.0f, false));
    assertThrows(
        IllegalArgumentException.class,
        () ->
            new InteractionDefinition(id, partId, Vector3.ZERO, Vector3.ZERO, 1.0f, -1.0f, false));
    assertThrows(
        IllegalArgumentException.class,
        () ->
            new InteractionDefinition(
                id, partId, Vector3.ZERO, Vector3.ZERO, Float.NaN, 1.0f, false));
    assertThrows(
        IllegalArgumentException.class,
        () ->
            new InteractionDefinition(
                id, partId, Vector3.ZERO, Vector3.ZERO, 1.0f, Float.NaN, false));
    assertThrows(
        NullPointerException.class,
        () ->
            new InteractionDefinition(null, partId, Vector3.ZERO, Vector3.ZERO, 1.0f, 1.0f, false));
    assertThrows(
        NullPointerException.class,
        () -> new InteractionDefinition(id, partId, null, Vector3.ZERO, 1.0f, 1.0f, false));
    assertThrows(
        NullPointerException.class,
        () -> new InteractionDefinition(id, partId, Vector3.ZERO, null, 1.0f, 1.0f, false));
  }
}
