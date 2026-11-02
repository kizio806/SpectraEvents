package io.github.kizio806.spectraevents.platform.bukkit.loot;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.github.kizio806.spectraevents.application.config.registry.EventDefinitionRegistry;
import io.github.kizio806.spectraevents.application.execution.EventExecutionEngine;
import io.github.kizio806.spectraevents.application.execution.EventRuntimeStateStore;
import io.github.kizio806.spectraevents.application.port.EventInstanceRepository;
import io.github.kizio806.spectraevents.application.port.EventTaskScheduler;
import io.github.kizio806.spectraevents.application.port.PlatformActionPort;
import java.lang.reflect.Proxy;
import java.util.Optional;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.ItemDespawnEvent;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.plugin.Plugin;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class BukkitSharedLootListenerTest {

  private Plugin dummyPlugin;
  private EventInstanceRepository dummyRepository;
  private EventExecutionEngine dummyEngine;

  @BeforeEach
  void setUp() {
    dummyPlugin =
        (Plugin)
            Proxy.newProxyInstance(
                Plugin.class.getClassLoader(),
                new Class<?>[] {Plugin.class},
                (proxy, method, args) -> null);

    dummyRepository =
        (EventInstanceRepository)
            Proxy.newProxyInstance(
                EventInstanceRepository.class.getClassLoader(),
                new Class<?>[] {EventInstanceRepository.class},
                (proxy, method, args) -> {
                  if ("findActiveById".equals(method.getName())) {
                    return Optional.empty();
                  }
                  return null;
                });

    EventTaskScheduler dummyScheduler =
        (EventTaskScheduler)
            Proxy.newProxyInstance(
                EventTaskScheduler.class.getClassLoader(),
                new Class<?>[] {EventTaskScheduler.class},
                (proxy, method, args) -> null);

    PlatformActionPort dummyActionPort =
        (PlatformActionPort)
            Proxy.newProxyInstance(
                PlatformActionPort.class.getClassLoader(),
                new Class<?>[] {PlatformActionPort.class},
                (proxy, method, args) -> null);

    dummyEngine =
        new EventExecutionEngine(
            dummyRepository,
            new EventDefinitionRegistry(),
            dummyScheduler,
            dummyActionPort,
            new EventRuntimeStateStore());
  }

  @Test
  void rejectsNullConstructorArguments() {
    assertThrows(
        NullPointerException.class,
        () -> new BukkitSharedLootListener(null, dummyRepository, dummyEngine, (p, r) -> {}));
    assertThrows(
        NullPointerException.class,
        () -> new BukkitSharedLootListener(dummyPlugin, null, dummyEngine, (p, r) -> {}));
    assertThrows(
        NullPointerException.class,
        () -> new BukkitSharedLootListener(dummyPlugin, dummyRepository, null, (p, r) -> {}));
    assertThrows(
        NullPointerException.class,
        () -> new BukkitSharedLootListener(dummyPlugin, dummyRepository, dummyEngine, null));
  }

  @Test
  void ignoresPickupForItemsWithoutPdc() {
    BukkitSharedLootListener listener =
        new BukkitSharedLootListener(dummyPlugin, dummyRepository, dummyEngine, (p, r) -> {});

    PersistentDataContainer pdc =
        (PersistentDataContainer)
            Proxy.newProxyInstance(
                PersistentDataContainer.class.getClassLoader(),
                new Class<?>[] {PersistentDataContainer.class},
                (proxy, method, args) -> {
                  if ("get".equals(method.getName())) {
                    return null;
                  }
                  return null;
                });

    Item dummyItem =
        (Item)
            Proxy.newProxyInstance(
                Item.class.getClassLoader(),
                new Class<?>[] {Item.class},
                (proxy, method, args) -> {
                  if ("getPersistentDataContainer".equals(method.getName())) {
                    return pdc;
                  }
                  return null;
                });

    Player dummyPlayer =
        (Player)
            Proxy.newProxyInstance(
                Player.class.getClassLoader(),
                new Class<?>[] {Player.class},
                (proxy, method, args) -> null);

    EntityPickupItemEvent event = new EntityPickupItemEvent(dummyPlayer, dummyItem, 0);
    listener.onGroundLootPickup(event);
    assertFalse(event.isCancelled());
  }

  @Test
  void ignoresDespawnForItemsWithoutPdc() {
    BukkitSharedLootListener listener =
        new BukkitSharedLootListener(dummyPlugin, dummyRepository, dummyEngine, (p, r) -> {});

    PersistentDataContainer pdc =
        (PersistentDataContainer)
            Proxy.newProxyInstance(
                PersistentDataContainer.class.getClassLoader(),
                new Class<?>[] {PersistentDataContainer.class},
                (proxy, method, args) -> null);

    Item dummyItem =
        (Item)
            Proxy.newProxyInstance(
                Item.class.getClassLoader(),
                new Class<?>[] {Item.class},
                (proxy, method, args) -> {
                  if ("getPersistentDataContainer".equals(method.getName())) {
                    return pdc;
                  }
                  return null;
                });

    org.bukkit.Location loc = new org.bukkit.Location(null, 0, 0, 0);
    ItemDespawnEvent event = new ItemDespawnEvent(dummyItem, loc);
    listener.onGroundLootDespawn(event);
    assertFalse(event.isCancelled());
  }
}
