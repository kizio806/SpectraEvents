package io.github.kizio806.spectraevents.platform.paper.gui;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.kizio806.spectraevents.application.config.locale.FileSystemLocaleLoader;
import io.github.kizio806.spectraevents.application.config.locale.LocaleCatalog;
import io.github.kizio806.spectraevents.application.config.registry.EventDefinitionRegistry;
import io.github.kizio806.spectraevents.application.integration.IntegrationRegistry;
import io.github.kizio806.spectraevents.application.repository.InMemoryEventInstanceRepository;
import io.github.kizio806.spectraevents.application.service.EventOrchestrationService;
import io.github.kizio806.spectraevents.application.update.UpdateInfo;
import io.github.kizio806.spectraevents.application.update.UpdatePort;
import io.github.kizio806.spectraevents.application.update.UpdateService;
import io.github.kizio806.spectraevents.platform.paper.config.PaperEventSettingsStore;
import java.nio.file.Path;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class AdminGuiControllerSessionTest {

  @TempDir Path temporaryDirectory;

  private final UpdatePort dummyUpdatePort =
      new UpdatePort() {
        @Override
        public CompletableFuture<UpdateInfo> checkUpdate(String version, String channel) {
          return CompletableFuture.completedFuture(UpdateInfo.upToDate(version));
        }

        @Override
        public CompletableFuture<Boolean> downloadUpdate(
            String downloadUrl, String targetFileName) {
          return CompletableFuture.completedFuture(false);
        }
      };

  @Test
  void constructorValidatesNullArguments() throws Exception {
    EventDefinitionRegistry defs = new EventDefinitionRegistry();
    InMemoryEventInstanceRepository repo = new InMemoryEventInstanceRepository();
    IntegrationRegistry integrations = new IntegrationRegistry();
    UpdateService updates = new UpdateService("1.0.0", dummyUpdatePort);
    EventOrchestrationService orch = new EventOrchestrationService(repo, defs);
    PaperEventSettingsStore store = new PaperEventSettingsStore(temporaryDirectory);
    LocaleCatalog locales = new FileSystemLocaleLoader(temporaryDirectory).loadCatalog("en-US");

    assertThrows(
        NullPointerException.class,
        () -> new AdminGuiController(null, repo, integrations, updates, orch, store, locales));
    assertThrows(
        NullPointerException.class,
        () -> new AdminGuiController(defs, null, integrations, updates, orch, store, locales));
  }

  @Test
  void reportsNoOpenSessionForUnknownPlayer() throws Exception {
    EventDefinitionRegistry defs = new EventDefinitionRegistry();
    InMemoryEventInstanceRepository repo = new InMemoryEventInstanceRepository();
    IntegrationRegistry integrations = new IntegrationRegistry();
    UpdateService updates = new UpdateService("1.0.0", dummyUpdatePort);
    EventOrchestrationService orch = new EventOrchestrationService(repo, defs);
    PaperEventSettingsStore store = new PaperEventSettingsStore(temporaryDirectory);
    LocaleCatalog locales = new FileSystemLocaleLoader(temporaryDirectory).loadCatalog("en-US");

    AdminGuiController controller =
        new AdminGuiController(defs, repo, integrations, updates, orch, store, locales);
    UUID playerId = UUID.randomUUID();

    assertFalse(controller.hasOpenSession(playerId));
    assertTrue(controller.getOpenSession(playerId).isEmpty());
  }
}
