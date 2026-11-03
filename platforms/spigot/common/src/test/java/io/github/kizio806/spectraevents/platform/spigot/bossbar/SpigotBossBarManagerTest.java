package io.github.kizio806.spectraevents.platform.spigot.bossbar;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.github.kizio806.spectraevents.application.execution.EventRuntimeState;
import io.github.kizio806.spectraevents.core.event.runtime.EventInstance;
import io.github.kizio806.spectraevents.core.event.runtime.EventInstanceId;
import java.lang.reflect.Proxy;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SpigotBossBarManagerTest {

  private SpigotBossBarManager manager;

  @BeforeEach
  void setUp() {
    manager = new SpigotBossBarManager((template, instance) -> template != null ? template : "");
  }

  @Test
  void rejectsNullTemplateRenderer() {
    assertThrows(NullPointerException.class, () -> new SpigotBossBarManager(null));
  }

  @Test
  void reportsFalseForNonExistentBossBar() {
    EventInstanceId id = EventInstanceId.generate();
    assertFalse(manager.hasBossBar(id));
  }

  @Test
  void returnsDefaultProgressModeWhenAbsent() {
    EventInstanceId id = EventInstanceId.generate();
    assertEquals("1.0", manager.getProgressMode(id));
  }

  @Test
  void handlesGracefulShutdownAndUnknownRemovals() {
    EventInstanceId id = EventInstanceId.generate();
    assertDoesNotThrow(() -> manager.removeBossBar(id));
    assertDoesNotThrow(() -> manager.cleanup(id));
    assertDoesNotThrow(manager::shutdown);
    assertFalse(manager.hasBossBar(id));
  }

  @Test
  void onPlayerJoinWithNoActiveBarsDoesNotThrow() {
    Player dummyPlayer =
        (Player)
            Proxy.newProxyInstance(
                Player.class.getClassLoader(),
                new Class<?>[] {Player.class},
                (proxy, method, args) -> null);

    assertDoesNotThrow(() -> manager.onPlayerJoin(dummyPlayer));
  }

  @Test
  void validatesNullArgumentsOnShowAndUpdate() {
    EventInstanceId id = EventInstanceId.generate();
    EventRuntimeState state = new EventRuntimeState(id);

    assertThrows(
        NullPointerException.class,
        () -> manager.showBossBar(null, state, "title", "PURPLE", "SOLID", "1.0", 1.0f));
    assertThrows(
        NullPointerException.class,
        () ->
            manager.showBossBar(
                EventInstance.create(
                    id,
                    new io.github.kizio806.spectraevents.core.event.definition.EventDefinitionId(
                        "test")),
                null,
                "title",
                "PURPLE",
                "SOLID",
                "1.0",
                1.0f));

    assertThrows(
        NullPointerException.class, () -> manager.updateBossBar(null, state, "title", "1.0", 1.0f));
    assertThrows(
        NullPointerException.class,
        () ->
            manager.updateBossBar(
                EventInstance.create(
                    id,
                    new io.github.kizio806.spectraevents.core.event.definition.EventDefinitionId(
                        "test")),
                null,
                "title",
                "1.0",
                1.0f));
  }
}
