package io.github.kizio806.spectraevents.platform.spigot.command;

import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.kizio806.spectraevents.application.config.locale.LocaleCatalog;
import io.github.kizio806.spectraevents.application.integration.IntegrationRegistry;
import io.github.kizio806.spectraevents.application.integration.IntegrationState;
import io.github.kizio806.spectraevents.platform.bukkit.testutil.MockBukkitSetup;
import java.util.Map;
import org.bukkit.command.ConsoleCommandSender;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SpigotIntegrationCommandHandlerTest {

  @BeforeAll
  static void beforeAll() {
    MockBukkitSetup.ensureInitialized();
  }

  private IntegrationRegistry registry;
  private SpigotCommandHelper helper;
  private SpigotIntegrationCommandHandler handler;

  @BeforeEach
  void setUp() {
    registry = new IntegrationRegistry();
    LocaleCatalog locales =
        LocaleCatalog.of(
            Map.of(
                "command.integrations.title", "Integrations (%count%):",
                "command.integrations.entry", "%name%: %state% (%details%)"));
    helper = new SpigotCommandHelper(locales);
    handler = new SpigotIntegrationCommandHandler(registry, helper);
  }

  @Test
  void shouldListIntegrations() {
    registry.register("Vault", IntegrationState.AVAILABLE, "Economy ready");
    registry.register("LuckPerms", IntegrationState.ABSENT, "Not found");

    ConsoleCommandSender console = org.bukkit.Bukkit.getConsoleSender();
    handler.execute(console, new String[0]);

    assertTrue(handler.tabComplete(console, new String[0]).isEmpty());
  }
}
