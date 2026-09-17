package io.github.kizio806.spectraevents.platform.spigot.metadata;

import org.bukkit.NamespacedKey;
import org.bukkit.plugin.Plugin;

/** Persistent Data Container keys used by SpectraEvents Spigot platform adapter. */
public final class SpigotPdcKeys {
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

  public static NamespacedKey instanceId(Plugin plugin) {
    return EVENT_INSTANCE_ID;
  }

  public static NamespacedKey partId(Plugin plugin) {
    return MODEL_PART_ID;
  }

  public static NamespacedKey entityRole(Plugin plugin) {
    return RESOURCE_ROLE;
  }

  private SpigotPdcKeys() {}
}
