package io.github.kizio806.spectraevents.platform.bukkit.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.kizio806.spectraevents.application.execution.EventRuntimeState;
import io.github.kizio806.spectraevents.application.execution.ExecutionContext;
import io.github.kizio806.spectraevents.core.event.definition.EventDefinitionId;
import io.github.kizio806.spectraevents.core.event.execution.action.IntegrationActions;
import io.github.kizio806.spectraevents.core.event.execution.action.PlatformActions;
import io.github.kizio806.spectraevents.core.event.phase.PhaseId;
import io.github.kizio806.spectraevents.core.event.runtime.EventInstance;
import io.github.kizio806.spectraevents.core.event.runtime.EventInstanceId;
import io.github.kizio806.spectraevents.core.event.runtime.EventLifecycleState;
import io.github.kizio806.spectraevents.platform.bukkit.testutil.MockBukkitSetup;
import java.util.UUID;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class VaultIntegrationTest {

  @BeforeAll
  static void setUp() {
    MockBukkitSetup.ensureInitialized();
  }

  @Test
  void testMetadataAndSupport() {
    VaultIntegration integration = new VaultIntegration();
    assertEquals("Vault", integration.requiredPluginName());
    assertTrue(integration.supports("give_money"));
    assertTrue(integration.supports("take_money"));
    assertFalse(integration.supports("unknown_action"));
  }

  @Test
  void testExecuteWithNullOrInvalidInputs() {
    VaultIntegration integration = new VaultIntegration();
    EventInstanceId id = EventInstanceId.generate();
    EventInstance instance =
        EventInstance.reconstitute(
            id, new EventDefinitionId("test"), EventLifecycleState.RUNNING, new PhaseId("p1"));
    EventRuntimeState state = new EventRuntimeState(id);

    assertThrows(
        NullPointerException.class,
        () -> integration.execute(new IntegrationActions.GiveMoneyAction(10.0), null, state, null));
    assertThrows(
        NullPointerException.class,
        () ->
            integration.execute(
                new IntegrationActions.GiveMoneyAction(10.0), instance, null, null));

    // Null context or null actor returns false
    assertFalse(
        integration
            .execute(new IntegrationActions.GiveMoneyAction(10.0), instance, state, null)
            .join());
    assertFalse(
        integration
            .execute(
                new IntegrationActions.GiveMoneyAction(10.0),
                instance,
                state,
                ExecutionContext.EMPTY)
            .join());

    // Non-player, non-UUID actor returns false
    assertFalse(
        integration
            .execute(
                new IntegrationActions.GiveMoneyAction(10.0),
                instance,
                state,
                ExecutionContext.withActor("invalid-actor"))
            .join());

    // Unsupported action or invalid amount
    assertFalse(
        integration
            .execute(
                new PlatformActions.BroadcastMessageAction("msg"),
                instance,
                state,
                ExecutionContext.withActor(UUID.randomUUID()))
            .join());
    assertFalse(
        integration
            .execute(
                new IntegrationActions.GiveMoneyAction(-5.0),
                instance,
                state,
                ExecutionContext.withActor(UUID.randomUUID()))
            .join());
    assertFalse(
        integration
            .execute(
                new IntegrationActions.GiveMoneyAction(Double.NaN),
                instance,
                state,
                ExecutionContext.withActor(UUID.randomUUID()))
            .join());
  }

  @Test
  void testInitializeRegistersActionResolver() {
    VaultIntegration integration = new VaultIntegration();
    io.github.kizio806.spectraevents.application.repository.InMemoryEventInstanceRepository
        repository =
            new io.github.kizio806.spectraevents.application.repository
                .InMemoryEventInstanceRepository();

    io.github.kizio806.spectraevents.application.SpectraEventsApplication app =
        new io.github.kizio806.spectraevents.application.SpectraEventsApplication(
            (io.github.kizio806.spectraevents.application.port.LifecycleReporter)
                java.lang.reflect.Proxy.newProxyInstance(
                    io.github.kizio806.spectraevents.application.port.LifecycleReporter.class
                        .getClassLoader(),
                    new Class<?>[] {
                      io.github.kizio806.spectraevents.application.port.LifecycleReporter.class
                    },
                    (p, m, a) -> null),
            (io.github.kizio806.spectraevents.application.port.EventTaskScheduler)
                java.lang.reflect.Proxy.newProxyInstance(
                    io.github.kizio806.spectraevents.application.port.EventTaskScheduler.class
                        .getClassLoader(),
                    new Class<?>[] {
                      io.github.kizio806.spectraevents.application.port.EventTaskScheduler.class
                    },
                    (p, m, a) -> null),
            (io.github.kizio806.spectraevents.application.port.PlatformActionPort)
                java.lang.reflect.Proxy.newProxyInstance(
                    io.github.kizio806.spectraevents.application.port.PlatformActionPort.class
                        .getClassLoader(),
                    new Class<?>[] {
                      io.github.kizio806.spectraevents.application.port.PlatformActionPort.class
                    },
                    (p, m, a) -> null),
            repository,
            (io.github.kizio806.spectraevents.application.port.PlatformEntityReconcilerPort)
                java.lang.reflect.Proxy.newProxyInstance(
                    io.github.kizio806.spectraevents.application.port.PlatformEntityReconcilerPort
                        .class
                        .getClassLoader(),
                    new Class<?>[] {
                      io.github.kizio806.spectraevents.application.port.PlatformEntityReconcilerPort
                          .class
                    },
                    (p, m, a) -> null),
            (io.github.kizio806.spectraevents.application.port.PlatformCapabilityQuery)
                java.lang.reflect.Proxy.newProxyInstance(
                    io.github.kizio806.spectraevents.application.port.PlatformCapabilityQuery.class
                        .getClassLoader(),
                    new Class<?>[] {
                      io.github.kizio806.spectraevents.application.port.PlatformCapabilityQuery
                          .class
                    },
                    (p, m, a) -> null),
            (io.github.kizio806.spectraevents.application.port.ModelRendererPort)
                java.lang.reflect.Proxy.newProxyInstance(
                    io.github.kizio806.spectraevents.application.port.ModelRendererPort.class
                        .getClassLoader(),
                    new Class<?>[] {
                      io.github.kizio806.spectraevents.application.port.ModelRendererPort.class
                    },
                    (p, m, a) -> null));

    io.github.kizio806.spectraevents.application.integration.IntegrationInitializationContext
        context =
            new io.github.kizio806.spectraevents.application.integration
                .IntegrationInitializationContext(
                app, repository, java.util.logging.Logger.getLogger("test"));

    org.junit.jupiter.api.Assertions.assertDoesNotThrow(() -> integration.initialize(context));
  }
}
