package io.github.kizio806.spectraevents.application.port;

import io.github.kizio806.spectraevents.application.execution.EventRuntimeState;
import io.github.kizio806.spectraevents.application.execution.ExecutionContext;
import io.github.kizio806.spectraevents.core.event.execution.action.ActionDefinition;
import io.github.kizio806.spectraevents.core.event.runtime.EventInstance;
import io.github.kizio806.spectraevents.core.event.runtime.EventInstanceId;
import io.github.kizio806.spectraevents.core.gameplay.reward.RewardItem;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;

/**
 * Port implemented by platform adapters to execute domain actions that require platform
 * side-effects. The core domain emits Actions (e.g., spawn_model, play_sound), and the Application
 * orchestrates them through this port.
 */
public interface PlatformActionPort {

  /**
   * Executes a platform action.
   *
   * @param instance the runtime instance of the event triggering the action
   * @param state the per-instance runtime state
   * @param action the action to execute
   */
  CompletableFuture<Boolean> executeAction(
      EventInstance instance, EventRuntimeState state, ActionDefinition action);

  /** Executes a platform action with execution context. */
  default CompletableFuture<Boolean> executeAction(
      EventInstance instance,
      EventRuntimeState state,
      ActionDefinition action,
      ExecutionContext context) {
    return executeAction(instance, state, action);
  }

  /** Removes all platform resources owned by one event instance. */
  default void cleanupEvent(EventInstanceId instanceId) {}

  /** Removes all platform resources owned by the plugin during shutdown. */
  default void cleanupAll() {}

  /** Returns tracked platform resources for diagnostics, or {@code -1} when unavailable. */
  default int resourceCount(EventInstanceId instanceId) {
    return -1;
  }

  /** Refreshes a compact global event HUD snapshot. Called at most once per second per event. */
  default void refreshHud(EventInstance instance, EventRuntimeState state) {}

  /**
   * Delivers an event reward to an online player, dropping the complete reward snapshot at the
   * player's location when it cannot fit in their inventory.
   *
   * <p>A {@code false} result means the player was unavailable, so the application retains the
   * durable mailbox claim for a later delivery.
   */
  default CompletableFuture<Boolean> deliverRewardOrDrop(UUID playerId, List<RewardItem> items) {
    return CompletableFuture.completedFuture(false);
  }

  /** Registers the engine callback used when an asynchronously scheduled platform action fails. */
  default void setFatalActionHandler(BiConsumer<EventInstanceId, Throwable> handler) {}
}
