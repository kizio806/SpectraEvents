package io.github.kizio806.spectraevents.application.port;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.kizio806.spectraevents.application.execution.EventRuntimeState;
import io.github.kizio806.spectraevents.application.execution.ExecutionContext;
import io.github.kizio806.spectraevents.core.event.definition.EventDefinitionId;
import io.github.kizio806.spectraevents.core.event.execution.action.ActionDefinition;
import io.github.kizio806.spectraevents.core.event.runtime.EventInstance;
import io.github.kizio806.spectraevents.core.event.runtime.EventInstanceId;
import io.github.kizio806.spectraevents.core.gameplay.reward.RewardItem;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.Test;

class PlatformActionPortTest {

  static class DummyPlatformActionPort implements PlatformActionPort {
    @Override
    public CompletableFuture<Boolean> executeAction(
        EventInstance instance, EventRuntimeState state, ActionDefinition action) {
      return CompletableFuture.completedFuture(true);
    }
  }

  @Test
  void testDefaultMethods() {
    DummyPlatformActionPort port = new DummyPlatformActionPort();
    EventInstanceId instanceId = EventInstanceId.generate();
    EventInstance instance = EventInstance.create(instanceId, new EventDefinitionId("test_event"));
    EventRuntimeState state = new EventRuntimeState(instanceId);
    ActionDefinition action = () -> "dummy";
    ExecutionContext context = ExecutionContext.EMPTY;

    assertTrue(port.executeAction(instance, state, action, context).join());

    port.cleanupEvent(instanceId);
    port.cleanupAll();

    assertEquals(-1, port.resourceCount(instanceId));

    port.refreshHud(instance, state);

    assertFalse(
        port.deliverRewardOrDrop(UUID.randomUUID(), List.of(new RewardItem("diamond", 1))).join());

    assertTrue(port.announcePodium(instance, List.of(UUID.randomUUID())).join());
    assertTrue(port.announcePodium(instanceId, List.of(UUID.randomUUID())).join());

    port.setFatalActionHandler((id, ex) -> {});
  }

  @Test
  void testNullValidations() {
    DummyPlatformActionPort port = new DummyPlatformActionPort();
    EventInstanceId instanceId = EventInstanceId.generate();
    EventInstance instance = EventInstance.create(instanceId, new EventDefinitionId("test_event"));
    EventRuntimeState state = new EventRuntimeState(instanceId);
    ActionDefinition action = () -> "dummy";

    assertThrows(
        NullPointerException.class, () -> port.executeAction(instance, state, action, null));
    assertThrows(NullPointerException.class, () -> port.cleanupEvent(null));
    assertThrows(NullPointerException.class, () -> port.resourceCount(null));
    assertThrows(NullPointerException.class, () -> port.refreshHud(null, state));
    assertThrows(NullPointerException.class, () -> port.refreshHud(instance, null));
    assertThrows(NullPointerException.class, () -> port.deliverRewardOrDrop(null, List.of()));
    assertThrows(
        NullPointerException.class, () -> port.deliverRewardOrDrop(UUID.randomUUID(), null));
    assertThrows(
        NullPointerException.class, () -> port.announcePodium((EventInstance) null, List.of()));
    assertThrows(
        NullPointerException.class, () -> port.announcePodium((EventInstanceId) null, List.of()));
    assertThrows(NullPointerException.class, () -> port.announcePodium(instanceId, null));
    assertThrows(NullPointerException.class, () -> port.setFatalActionHandler(null));
  }
}
