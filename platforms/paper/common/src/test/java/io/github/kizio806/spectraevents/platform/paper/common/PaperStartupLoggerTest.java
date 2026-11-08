package io.github.kizio806.spectraevents.platform.paper.common;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.kizio806.spectraevents.application.integration.IntegrationRegistry;
import io.github.kizio806.spectraevents.application.integration.IntegrationState;
import java.lang.reflect.Proxy;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.logging.Handler;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import org.bukkit.plugin.Plugin;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PaperStartupLoggerTest {

  private List<String> logMessages;
  private Logger logger;
  private Plugin mockPlugin;
  private PaperStartupLogger startupLogger;

  @BeforeEach
  void setUp() {
    logMessages = new ArrayList<>();
    logger = Logger.getLogger("PaperStartupLoggerTest-" + System.nanoTime());
    logger.setUseParentHandlers(false);
    logger.addHandler(
        new Handler() {
          @Override
          public void publish(LogRecord record) {
            logMessages.add(record.getMessage());
          }

          @Override
          public void flush() {}

          @Override
          public void close() {}
        });

    mockPlugin =
        (Plugin)
            Proxy.newProxyInstance(
                Plugin.class.getClassLoader(),
                new Class<?>[] {Plugin.class},
                (proxy, method, args) -> {
                  if ("getLogger".equals(method.getName())) return logger;
                  return null;
                });

    startupLogger = new PaperStartupLogger(mockPlugin);
  }

  @Test
  void testPrintBanner() {
    startupLogger.printBanner("1.0.0", "Paper", "26.1.0");
    assertTrue(logMessages.stream().anyMatch(msg -> msg.contains("Version:   1.0.0")));
    assertTrue(logMessages.stream().anyMatch(msg -> msg.contains("Platform:  Paper 26.1.0")));
  }

  @Test
  void testStorage() {
    startupLogger.storage(Path.of("test.db"), 5);
    assertTrue(logMessages.stream().anyMatch(msg -> msg.contains("5 persisted instances")));
  }

  @Test
  void testModels() {
    startupLogger.models(10, 2);
    assertTrue(logMessages.stream().anyMatch(msg -> msg.contains("10 model definitions")));
  }

  @Test
  void testDefinitionsWithSuccessAndFailures() {
    startupLogger.definitions(Set.of("event_a", "event_b"), 1);
    assertTrue(logMessages.stream().anyMatch(msg -> msg.contains("2 event definitions")));
    assertTrue(logMessages.stream().anyMatch(msg -> msg.contains("1 definition files rejected")));
  }

  @Test
  void testIntegrations() {
    IntegrationRegistry registry = new IntegrationRegistry();
    registry.register("Vault", IntegrationState.ACTIVE, "1.7");
    registry.register("LuckPerms", IntegrationState.AVAILABLE, "5.4");
    registry.register("PlaceholderAPI", IntegrationState.DEGRADED, "2.11");
    registry.register("WorldGuard", IntegrationState.FAILED, "7.0");
    registry.register("DecentHolograms", IntegrationState.ABSENT, "2.8");

    assertDoesNotThrow(() -> startupLogger.integrations(registry));
    assertTrue(
        logMessages.stream().anyMatch(msg -> msg.contains("Vault") && msg.contains("ACTIVE")));
    assertTrue(
        logMessages.stream()
            .anyMatch(msg -> msg.contains("LuckPerms") && msg.contains("AVAILABLE")));
    assertTrue(
        logMessages.stream()
            .anyMatch(msg -> msg.contains("PlaceholderAPI") && msg.contains("DEGRADED")));
    assertTrue(
        logMessages.stream().anyMatch(msg -> msg.contains("WorldGuard") && msg.contains("FAILED")));
    assertTrue(
        logMessages.stream()
            .anyMatch(msg -> msg.contains("DecentHolograms") && msg.contains("NOT INSTALLED")));
  }

  @Test
  void testCheckingForUpdatesAndEnabled() {
    startupLogger.checkingForUpdates();
    startupLogger.enabled(123);
    assertTrue(logMessages.stream().anyMatch(msg -> msg.contains("Checking for updates...")));
    assertTrue(logMessages.stream().anyMatch(msg -> msg.contains("123 ms")));
  }

  @Test
  void describesTheRunningServerInsteadOfTheArtifactApiVersion() {
    assertEquals("Purpur 26.3", PaperStartupLogger.platformDescription("Purpur", "26.3"));
  }
}
