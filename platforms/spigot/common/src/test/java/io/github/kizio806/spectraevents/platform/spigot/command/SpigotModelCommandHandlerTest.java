package io.github.kizio806.spectraevents.platform.spigot.command;

import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.kizio806.spectraevents.application.config.locale.LocaleCatalog;
import io.github.kizio806.spectraevents.application.model.registry.ModelDefinitionRegistry;
import io.github.kizio806.spectraevents.core.visual.model.DisplayTransformMode;
import io.github.kizio806.spectraevents.core.visual.model.ItemAssetRef;
import io.github.kizio806.spectraevents.core.visual.model.ModelDefinition;
import io.github.kizio806.spectraevents.core.visual.model.ModelId;
import io.github.kizio806.spectraevents.core.visual.model.ModelPartDefinition;
import io.github.kizio806.spectraevents.core.visual.model.ModelPartId;
import io.github.kizio806.spectraevents.core.visual.model.ModelPartType;
import io.github.kizio806.spectraevents.core.visual.model.ModelRenderProperties;
import io.github.kizio806.spectraevents.core.visual.model.ModelTransform;
import io.github.kizio806.spectraevents.platform.bukkit.testutil.MockBukkitSetup;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.bukkit.command.ConsoleCommandSender;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SpigotModelCommandHandlerTest {

  @BeforeAll
  static void beforeAll() {
    MockBukkitSetup.ensureInitialized();
  }

  private ModelDefinitionRegistry registry;
  private SpigotCommandHelper helper;
  private SpigotModelCommandHandler handler;

  @BeforeEach
  void setUp() {
    registry = new ModelDefinitionRegistry();
    LocaleCatalog locales =
        LocaleCatalog.of(
            Map.of(
                "command.models.title", "Models (%count%):",
                "command.models.entry", "%id%: parts=%parts%, interactions=%interactions%"));
    helper = new SpigotCommandHelper(locales);
    handler = new SpigotModelCommandHandler(registry, helper);
  }

  @Test
  void shouldListModels() {
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
        new ModelDefinition(new ModelId("metin_stone"), List.of(part), Collections.emptyList());
    registry.register(model);

    ConsoleCommandSender console = org.bukkit.Bukkit.getConsoleSender();
    handler.execute(console, new String[0]);

    assertTrue(handler.tabComplete(console, new String[0]).isEmpty());
  }
}
