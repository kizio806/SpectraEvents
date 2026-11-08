package io.github.kizio806.spectraevents.platform.bukkit.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import io.github.kizio806.spectraevents.application.port.EventInstanceRepository;
import io.github.kizio806.spectraevents.core.event.definition.EventDefinitionId;
import io.github.kizio806.spectraevents.core.event.phase.PhaseId;
import io.github.kizio806.spectraevents.core.event.runtime.EventInstance;
import io.github.kizio806.spectraevents.core.event.runtime.EventInstanceId;
import io.github.kizio806.spectraevents.core.event.runtime.EventLifecycleState;
import java.lang.reflect.Proxy;
import java.util.List;
import org.junit.jupiter.api.Test;

class PlaceholderAPIIntegrationTest {

  @Test
  void testMetadataAndExpansionRequests() {
    PlaceholderAPIIntegration integration = new PlaceholderAPIIntegration();
    assertEquals("PlaceholderAPI", integration.requiredPluginName());

    EventInstance running1 =
        EventInstance.reconstitute(
            EventInstanceId.generate(),
            new EventDefinitionId("def1"),
            EventLifecycleState.RUNNING,
            new PhaseId("p1"));
    EventInstance completed =
        EventInstance.reconstitute(
            EventInstanceId.generate(),
            new EventDefinitionId("def2"),
            EventLifecycleState.COMPLETED,
            new PhaseId("p2"));

    EventInstanceRepository repo =
        (EventInstanceRepository)
            Proxy.newProxyInstance(
                EventInstanceRepository.class.getClassLoader(),
                new Class<?>[] {EventInstanceRepository.class},
                (proxy, method, args) -> {
                  if ("findAll".equals(method.getName())) {
                    return List.of(running1, completed);
                  }
                  return null;
                });

    PlaceholderAPIIntegration.SpectraExpansion expansion =
        new PlaceholderAPIIntegration.SpectraExpansion(repo);
    assertEquals("spectra", expansion.getIdentifier());
    assertEquals("SpectraTeam", expansion.getAuthor());
    assertEquals("1.0.0", expansion.getVersion());

    assertEquals("1", expansion.onRequest(null, "active_count"));
    assertNull(expansion.onRequest(null, "unsupported_placeholder"));
  }

  @Test
  void testInitialize() {
    PlaceholderAPIIntegration integration = new PlaceholderAPIIntegration();
    java.util.logging.Logger logger = java.util.logging.Logger.getLogger("TestPapi");
    io.github.kizio806.spectraevents.application.integration.IntegrationInitializationContext
        context =
            new io.github.kizio806.spectraevents.application.integration
                .IntegrationInitializationContext(
                null,
                (EventInstanceRepository)
                    Proxy.newProxyInstance(
                        EventInstanceRepository.class.getClassLoader(),
                        new Class<?>[] {EventInstanceRepository.class},
                        (p, m, a) -> null),
                logger);

    // Call initialize - should catch gracefully if PlaceholderAPI is missing or register if present
    org.junit.jupiter.api.Assertions.assertDoesNotThrow(() -> integration.initialize(context));
  }
}
