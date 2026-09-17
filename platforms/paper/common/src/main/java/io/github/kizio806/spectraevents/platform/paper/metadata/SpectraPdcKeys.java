package io.github.kizio806.spectraevents.platform.paper.metadata;

import org.bukkit.NamespacedKey;
import org.bukkit.plugin.Plugin;

/** Persistent Data Container keys used by SpectraEvents Paper platform adapter. */
public final class SpectraPdcKeys {
  public static final NamespacedKey INSTANCE_ID = new NamespacedKey("spectraevents", "instance_id");
  public static final NamespacedKey MODEL_ID = new NamespacedKey("spectraevents", "model_id");

  public static NamespacedKey instanceId(Plugin plugin) {
    return INSTANCE_ID;
  }

  public static NamespacedKey partId(Plugin plugin) {
    return new NamespacedKey(plugin, "part_id");
  }

  public static NamespacedKey entityRole(Plugin plugin) {
    return new NamespacedKey(plugin, "entity_role");
  }

  private SpectraPdcKeys() {}
}
