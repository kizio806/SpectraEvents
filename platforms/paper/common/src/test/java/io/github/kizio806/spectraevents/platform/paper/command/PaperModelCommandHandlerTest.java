package io.github.kizio806.spectraevents.platform.paper.command;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.mojang.brigadier.CommandDispatcher;
import io.github.kizio806.spectraevents.application.config.locale.LocaleCatalog;
import io.github.kizio806.spectraevents.application.model.registry.ModelDefinitionRegistry;
import io.github.kizio806.spectraevents.core.visual.model.DisplayTransformMode;
import io.github.kizio806.spectraevents.core.visual.model.InteractionDefinition;
import io.github.kizio806.spectraevents.core.visual.model.InteractionId;
import io.github.kizio806.spectraevents.core.visual.model.ItemAssetRef;
import io.github.kizio806.spectraevents.core.visual.model.ModelDefinition;
import io.github.kizio806.spectraevents.core.visual.model.ModelId;
import io.github.kizio806.spectraevents.core.visual.model.ModelPartDefinition;
import io.github.kizio806.spectraevents.core.visual.model.ModelPartId;
import io.github.kizio806.spectraevents.core.visual.model.ModelPartType;
import io.github.kizio806.spectraevents.core.visual.model.ModelRenderProperties;
import io.github.kizio806.spectraevents.core.visual.model.ModelTransform;
import io.github.kizio806.spectraevents.core.visual.model.Vector3;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import net.kyori.adventure.text.Component;
import org.bukkit.command.CommandSender;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PaperModelCommandHandlerTest {

  private ModelDefinitionRegistry modelRegistry;
  private LocaleCatalog locales;
  private ModelCommandHandler handler;
  private List<Component> messages;
  private CommandDispatcher<CommandSourceStack> dispatcher;
  private CommandSourceStack source;

  @BeforeEach
  void setUp() {
    modelRegistry = new ModelDefinitionRegistry();
    locales =
        LocaleCatalog.of(
            Map.of(
                "command.models.list-title", "Models (%count%):",
                "command.models.entry", " - %id% (parts: %parts%, hitboxes: %hitboxes%)",
                "command.models.missing", "Model %id% not found",
                "command.models.info-title", "Model: %id%",
                "command.models.parts-title", "Parts (%count%):",
                "command.models.part-entry", " - %id% (type: %type%, parent: %parent%)",
                "command.models.hitboxes-title", "Hitboxes (%count%):",
                "command.models.hitbox-entry", " - %id% (%width%x%height%)"));

    handler = new ModelCommandHandler(modelRegistry, locales);
    messages = new ArrayList<>();
    CommandSender sender = PaperCommandTestSupport.createRecordingSender(messages);
    source = PaperCommandTestSupport.createMockSource(sender, null);

    dispatcher = PaperCommandTestSupport.createDispatcher();
    dispatcher.register(handler.build());
  }

  @Test
  void testListModels() throws Exception {
    ModelPartDefinition part =
        new ModelPartDefinition(
            new ModelPartId("head"),
            null,
            ModelPartType.ITEM_DISPLAY,
            ModelTransform.IDENTITY,
            ModelTransform.IDENTITY,
            ModelRenderProperties.DEFAULT,
            new ItemAssetRef("minecraft:diamond_helmet", DisplayTransformMode.HEAD));
    ModelDefinition model =
        new ModelDefinition(new ModelId("custom_boss"), List.of(part), Collections.emptyList());
    modelRegistry.register(model);

    dispatcher.execute("model list", source);
    assertFalse(messages.isEmpty());
  }

  @Test
  void testModelInfoFoundAndNotFound() throws Exception {
    ModelPartDefinition rootPart =
        new ModelPartDefinition(
            new ModelPartId("body"),
            null,
            ModelPartType.ITEM_DISPLAY,
            ModelTransform.IDENTITY,
            ModelTransform.IDENTITY,
            ModelRenderProperties.DEFAULT,
            new ItemAssetRef("minecraft:diamond_chestplate", DisplayTransformMode.HEAD));
    ModelPartDefinition childPart =
        new ModelPartDefinition(
            new ModelPartId("head"),
            new ModelPartId("body"),
            ModelPartType.ITEM_DISPLAY,
            ModelTransform.IDENTITY,
            ModelTransform.IDENTITY,
            ModelRenderProperties.DEFAULT,
            new ItemAssetRef("minecraft:diamond_helmet", DisplayTransformMode.HEAD));
    InteractionDefinition interaction =
        new InteractionDefinition(
            new InteractionId("main_hitbox"),
            new ModelPartId("body"),
            Vector3.ZERO,
            Vector3.ZERO,
            1.5f,
            2.0f,
            true);
    ModelDefinition model =
        new ModelDefinition(
            new ModelId("custom_boss"), List.of(rootPart, childPart), List.of(interaction));
    modelRegistry.register(model);

    // Found
    dispatcher.execute("model info \"custom_boss\"", source);
    assertFalse(messages.isEmpty());

    // Not found
    messages.clear();
    dispatcher.execute("model info \"missing_boss\"", source);
    assertFalse(messages.isEmpty());
  }

  @Test
  void testSuggestions() {
    ModelPartDefinition part =
        new ModelPartDefinition(
            new ModelPartId("head"),
            null,
            ModelPartType.ITEM_DISPLAY,
            ModelTransform.IDENTITY,
            ModelTransform.IDENTITY,
            ModelRenderProperties.DEFAULT,
            new ItemAssetRef("minecraft:diamond_helmet", DisplayTransformMode.HEAD));
    modelRegistry.register(
        new ModelDefinition(new ModelId("custom_boss"), List.of(part), List.of()));

    var suggestions =
        dispatcher.getCompletionSuggestions(dispatcher.parse("model info cust", source)).join();
    assertNotNull(suggestions);
    assertFalse(suggestions.getList().isEmpty());
  }
}
