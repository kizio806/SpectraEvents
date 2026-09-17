package io.github.kizio806.spectraevents.platform.paper.interaction;

import io.github.kizio806.spectraevents.core.event.runtime.EventInstance;
import org.bukkit.entity.Player;

/** Interface for components that can handle generic PDC-routed interactions. */
public interface EventInteractionDelegate {

  /**
   * Called when a player interacts with a part of a spawned model belonging to an event.
   *
   * @param player the player interacting
   * @param instance the active event instance being interacted with
   * @param phase the current phase of the event instance
   * @param modelIdStr the definition ID of the model
   */
  void handleInteraction(Player player, EventInstance instance, String phase, String modelIdStr);
}
