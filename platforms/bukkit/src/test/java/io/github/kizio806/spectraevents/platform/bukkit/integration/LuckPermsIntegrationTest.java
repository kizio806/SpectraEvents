package io.github.kizio806.spectraevents.platform.bukkit.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.kizio806.spectraevents.application.execution.EventRuntimeState;
import io.github.kizio806.spectraevents.application.execution.ExecutionContext;
import io.github.kizio806.spectraevents.core.event.definition.EventDefinitionId;
import io.github.kizio806.spectraevents.core.event.execution.condition.CoreConditions;
import io.github.kizio806.spectraevents.core.event.execution.condition.IntegrationConditions;
import io.github.kizio806.spectraevents.core.event.phase.PhaseId;
import io.github.kizio806.spectraevents.core.event.runtime.EventInstance;
import io.github.kizio806.spectraevents.core.event.runtime.EventInstanceId;
import io.github.kizio806.spectraevents.core.event.runtime.EventLifecycleState;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class LuckPermsIntegrationTest {

  @Test
  void testMetadataAndSupport() {
    LuckPermsIntegration integration = new LuckPermsIntegration();
    assertEquals("LuckPerms", integration.requiredPluginName());
    assertTrue(integration.supports("has_permission"));
    assertTrue(integration.supports("has_group"));
    assertFalse(integration.supports("unknown_condition"));
  }

  @Test
  void testResolveWithNullOrInvalidInputs() {
    LuckPermsIntegration integration = new LuckPermsIntegration();
    EventInstanceId id = EventInstanceId.generate();
    EventInstance instance =
        EventInstance.reconstitute(
            id, new EventDefinitionId("test"), EventLifecycleState.RUNNING, new PhaseId("p1"));
    EventRuntimeState state = new EventRuntimeState(id);

    assertThrows(
        NullPointerException.class,
        () ->
            integration.resolve(
                new IntegrationConditions.HasPermissionCondition("perm"), null, state, null));
    assertThrows(
        NullPointerException.class,
        () ->
            integration.resolve(
                new IntegrationConditions.HasPermissionCondition("perm"), instance, null, null));

    assertFalse(
        integration.resolve(
            new IntegrationConditions.HasPermissionCondition("perm"), instance, state, null));
    assertFalse(
        integration.resolve(
            new IntegrationConditions.HasPermissionCondition("perm"),
            instance,
            state,
            ExecutionContext.EMPTY));

    // Unknown condition returns false
    assertFalse(
        integration.resolve(
            new CoreConditions.NotLockedCondition(),
            instance,
            state,
            ExecutionContext.withActor(UUID.randomUUID())));
  }

  @Test
  void testInitializeRegistersConditionResolver() {
    LuckPermsIntegration integration = new LuckPermsIntegration();
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
