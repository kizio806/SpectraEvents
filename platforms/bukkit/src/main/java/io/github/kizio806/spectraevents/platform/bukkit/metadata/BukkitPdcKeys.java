package io.github.kizio806.spectraevents.platform.bukkit.metadata;

import org.bukkit.NamespacedKey;
import org.bukkit.plugin.Plugin;

/**
 * Unified Persistent Data Container keys used by Bukkit-family platform adapters (Paper and
 * Spigot).
 */
public final class BukkitPdcKeys {
  public static final NamespacedKey MODEL_INSTANCE_ID =
      new NamespacedKey("spectraevents", "model_instance_id");
  public static final NamespacedKey MODEL_DEFINITION_ID =
      new NamespacedKey("spectraevents", "model_definition_id");
  public static final NamespacedKey MODEL_PART_ID =
      new NamespacedKey("spectraevents", "model_part_id");
  public static final NamespacedKey EVENT_INSTANCE_ID =
      new NamespacedKey("spectraevents", "instance_id");
  public static final NamespacedKey RESOURCE_ROLE =
      new NamespacedKey("spectraevents", "resource_role");
  public static final NamespacedKey WAVE_ID = new NamespacedKey("spectraevents", "wave_id");
  public static final NamespacedKey GROUND_LOOT_SLOT =
      new NamespacedKey("spectraevents", "ground_loot_slot");

  public static NamespacedKey instanceId(Plugin plugin) {
    return new NamespacedKey(plugin, "instance_id");
  }

  public static NamespacedKey partId(Plugin plugin) {
    return new NamespacedKey(plugin, "model_part_id");
  }

  public static NamespacedKey entityRole(Plugin plugin) {
    return new NamespacedKey(plugin, "resource_role");
  }

  private BukkitPdcKeys() {}
}
