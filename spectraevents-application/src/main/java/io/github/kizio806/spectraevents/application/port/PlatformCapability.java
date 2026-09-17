package io.github.kizio806.spectraevents.application.port;

/**
 * Defines high-level technical capabilities of the server platform that the engine requires or
 * utilizes. This is used to determine if a specific event definition can run on the current server,
 * or if a graceful fallback should be used.
 */
public enum PlatformCapability {
  /**
   * The server natively supports Minecraft Display Entities (Text, Item, Block). Spigot 1.19.4+ /
   * Paper 1.19.4+ / SpongeAPI 10+.
   */
  DISPLAY_ENTITIES,

  /** The server natively supports Minecraft Interaction entities. */
  INTERACTION_ENTITIES,

  /**
   * The server executes actions across independent regions asynchronously (e.g. Folia). If true,
   * synchronous global actions must be handled with care.
   */
  REGION_SCHEDULING,

  /** The platform supports native Adventure component integration. */
  ADVENTURE_NATIVE,

  /** The platform supports creating an administrative graphical user interface (inventory GUI). */
  ADMIN_GUI,

  /** The platform supports providing custom items from external providers like Nexo or Oraxen. */
  CUSTOM_ITEMS
}
