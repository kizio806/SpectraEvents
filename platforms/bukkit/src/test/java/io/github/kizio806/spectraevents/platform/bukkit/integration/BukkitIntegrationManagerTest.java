package io.github.kizio806.spectraevents.platform.bukkit.integration;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.github.kizio806.spectraevents.application.SpectraEventsApplication;
import io.github.kizio806.spectraevents.application.integration.IntegrationInitializationContext;
import io.github.kizio806.spectraevents.application.integration.IntegrationRegistry;
import io.github.kizio806.spectraevents.application.integration.IntegrationState;
import io.github.kizio806.spectraevents.application.port.EventInstanceRepository;
import io.github.kizio806.spectraevents.application.port.EventTaskScheduler;
import io.github.kizio806.spectraevents.application.port.LifecycleReporter;
import io.github.kizio806.spectraevents.application.port.ModelRendererPort;
import io.github.kizio806.spectraevents.application.port.PlatformActionPort;
import io.github.kizio806.spectraevents.application.port.PlatformCapabilityQuery;
import io.github.kizio806.spectraevents.application.port.PlatformEntityReconcilerPort;
import io.github.kizio806.spectraevents.application.repository.InMemoryEventInstanceRepository;
import java.lang.reflect.Proxy;
import java.util.logging.Logger;
import org.bukkit.plugin.Plugin;
import org.junit.jupiter.api.Test;

class BukkitIntegrationManagerTest {

  @Test
  void rejectsNullRegistry() {
    assertThrows(NullPointerException.class, () -> new BukkitIntegrationManager(null));
  }

  @Test
  void exposesRegistry() {
    IntegrationRegistry registry = new IntegrationRegistry();
    BukkitIntegrationManager manager = new BukkitIntegrationManager(registry);
    assertSame(registry, manager.registry());
  }

  @Test
  void detectAllWithoutServerRegistersAbsentStates() {
    IntegrationRegistry registry = new IntegrationRegistry();
    BukkitIntegrationManager manager = new BukkitIntegrationManager(registry);

    manager.detectAll();

    assertEquals(IntegrationState.ABSENT, registry.get("LuckPerms").orElseThrow().state());
    assertEquals(IntegrationState.ABSENT, registry.get("WorldGuard").orElseThrow().state());
    assertEquals(IntegrationState.ABSENT, registry.get("Vault").orElseThrow().state());
    assertEquals(IntegrationState.ABSENT, registry.get("PlaceholderAPI").orElseThrow().state());
  }

  @Test
  void initializeModulesRejectsNullParameters() {
    IntegrationRegistry registry = new IntegrationRegistry();
    BukkitIntegrationManager manager = new BukkitIntegrationManager(registry);
    Plugin dummyPlugin = createDummyPlugin();
    IntegrationInitializationContext context =
        new IntegrationInitializationContext(
            createDummyApplication(), createDummyRepository(), Logger.getLogger("test"));

    assertThrows(NullPointerException.class, () -> manager.initializeModules(null, context));
    assertThrows(NullPointerException.class, () -> manager.initializeModules(dummyPlugin, null));
  }

  @Test
  void initializeModulesHandlesAbsentServerSafely() {
    IntegrationRegistry registry = new IntegrationRegistry();
    BukkitIntegrationManager manager = new BukkitIntegrationManager(registry);
    Plugin dummyPlugin = createDummyPlugin();
    IntegrationInitializationContext context =
        new IntegrationInitializationContext(
            createDummyApplication(), createDummyRepository(), Logger.getLogger("test"));

    assertDoesNotThrow(() -> manager.initializeModules(dummyPlugin, context));
  }

  private Plugin createDummyPlugin() {
    return (Plugin)
        Proxy.newProxyInstance(
            Plugin.class.getClassLoader(),
            new Class<?>[] {Plugin.class},
            (proxy, method, args) -> {
              if ("getLogger".equals(method.getName())) {
                return Logger.getLogger("test");
              }
              return null;
            });
  }

  private EventInstanceRepository createDummyRepository() {
    return (EventInstanceRepository)
        Proxy.newProxyInstance(
            EventInstanceRepository.class.getClassLoader(),
            new Class<?>[] {EventInstanceRepository.class},
            (proxy, method, args) -> null);
  }

  private SpectraEventsApplication createDummyApplication() {
    LifecycleReporter reporter =
        (LifecycleReporter)
            Proxy.newProxyInstance(
                LifecycleReporter.class.getClassLoader(),
                new Class<?>[] {LifecycleReporter.class},
                (proxy, method, args) -> null);
    EventTaskScheduler scheduler =
        (EventTaskScheduler)
            Proxy.newProxyInstance(
                EventTaskScheduler.class.getClassLoader(),
                new Class<?>[] {EventTaskScheduler.class},
                (proxy, method, args) -> null);
    PlatformActionPort actionPort =
        (PlatformActionPort)
            Proxy.newProxyInstance(
                PlatformActionPort.class.getClassLoader(),
                new Class<?>[] {PlatformActionPort.class},
                (proxy, method, args) -> null);
    InMemoryEventInstanceRepository repository = new InMemoryEventInstanceRepository();
    PlatformEntityReconcilerPort reconciler =
        (PlatformEntityReconcilerPort)
            Proxy.newProxyInstance(
                PlatformEntityReconcilerPort.class.getClassLoader(),
                new Class<?>[] {PlatformEntityReconcilerPort.class},
                (proxy, method, args) -> null);
    PlatformCapabilityQuery capability =
        (PlatformCapabilityQuery)
            Proxy.newProxyInstance(
                PlatformCapabilityQuery.class.getClassLoader(),
                new Class<?>[] {PlatformCapabilityQuery.class},
                (proxy, method, args) -> null);
    ModelRendererPort renderer =
        (ModelRendererPort)
            Proxy.newProxyInstance(
                ModelRendererPort.class.getClassLoader(),
                new Class<?>[] {ModelRendererPort.class},
                (proxy, method, args) -> null);

    return new SpectraEventsApplication(
        reporter, scheduler, actionPort, repository, reconciler, capability, renderer);
  }
}
