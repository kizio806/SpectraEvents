package io.github.kizio806.spectraevents.platform.paper.command;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import com.mojang.brigadier.CommandDispatcher;
import io.github.kizio806.spectraevents.application.config.locale.LocaleCatalog;
import io.github.kizio806.spectraevents.application.integration.IntegrationRegistry;
import io.github.kizio806.spectraevents.application.integration.IntegrationState;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.kyori.adventure.text.Component;
import org.bukkit.command.CommandSender;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PaperIntegrationCommandHandlerTest {

  private IntegrationRegistry registry;
  private LocaleCatalog locales;
  private IntegrationCommandHandler handler;
  private List<Component> messages;
  private CommandSourceStack source;

  @BeforeEach
  void setUp() {
    registry = new IntegrationRegistry();
    locales =
        LocaleCatalog.of(
            Map.of(
                "command.integrations.title", "Integrations (%count%):",
                "command.integrations.entry", " - %name%: ",
                "command.integrations.details", " (%details%)"));
    handler = new IntegrationCommandHandler(registry, locales);
    messages = new ArrayList<>();
    CommandSender sender = PaperCommandTestSupport.createRecordingSender(messages);
    source = PaperCommandTestSupport.createMockSource(sender, null);
  }

  @Test
  void testBuildAndExecuteList() throws Exception {
    registry.register("Vault", IntegrationState.ACTIVE, "1.7");
    registry.register("LuckPerms", IntegrationState.AVAILABLE, "5.4");
    registry.register("PlaceholderAPI", IntegrationState.DEGRADED, "2.11");
    registry.register("WorldGuard", IntegrationState.FAILED, "7.0");
    registry.register("DecentHolograms", IntegrationState.ABSENT, "2.8");

    CommandDispatcher<CommandSourceStack> dispatcher = PaperCommandTestSupport.createDispatcher();
    dispatcher.register(handler.build());

    int result = dispatcher.execute("integrations", source);
    assertEquals(1, result);
    assertFalse(messages.isEmpty());
  }
}
