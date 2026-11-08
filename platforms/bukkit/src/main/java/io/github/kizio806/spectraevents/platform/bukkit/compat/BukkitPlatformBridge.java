package io.github.kizio806.spectraevents.platform.bukkit.compat;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.damage.DamageSource;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPluginLoader;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;

/**
 * Architectural compatibility bridge abstracting platform divergence between modern Paper/Folia
 * (Adventure-native) and Spigot/CraftBukkit.
 *
 * <p>Encapsulates reflective version dispatch in a dedicated, isolated compatibility boundary,
 * preventing deprecated platform invocations and providing structured diagnostic reporting.
 */
public final class BukkitPlatformBridge {

  private static final Logger LOGGER = Logger.getLogger(BukkitPlatformBridge.class.getName());

  private static final boolean HAS_ADVENTURE_KICK;
  private static final boolean HAS_ADVENTURE_CUSTOM_NAME;
  private static final boolean HAS_ADVENTURE_INVENTORY;
  private static final boolean HAS_ADVENTURE_TEXT_DISPLAY;
  private static final boolean HAS_ADVENTURE_ITEM_META;
  private static final Constructor<PlayerJoinEvent> ADVENTURE_JOIN_CONSTRUCTOR;
  private static final Constructor<PlayerQuitEvent> ADVENTURE_QUIT_CONSTRUCTOR;

  static {
    boolean advItem = false;
    try {
      ItemMeta.class.getMethod("displayName", Component.class);
      advItem = true;
    } catch (ReflectiveOperationException ignored) {
      // Paper Adventure ItemMeta unavailable
    }
    HAS_ADVENTURE_ITEM_META = advItem;
    boolean advKick = false;
    try {
      Player.class.getMethod("kick", Component.class);
      advKick = true;
    } catch (ReflectiveOperationException ignored) {
      // Paper Adventure kick unavailable
    }
    HAS_ADVENTURE_KICK = advKick;

    boolean advCustomName = false;
    try {
      Entity.class.getMethod("customName", Component.class);
      advCustomName = true;
    } catch (ReflectiveOperationException ignored) {
      // Paper Adventure customName unavailable
    }
    HAS_ADVENTURE_CUSTOM_NAME = advCustomName;

    boolean advInv = false;
    try {
      Bukkit.class.getMethod("createInventory", InventoryHolder.class, int.class, Component.class);
      advInv = true;
    } catch (ReflectiveOperationException ignored) {
      // Paper Adventure inventory creation unavailable
    }
    HAS_ADVENTURE_INVENTORY = advInv;

    boolean advText = false;
    try {
      TextDisplay.class.getMethod("text", Component.class);
      advText = true;
    } catch (ReflectiveOperationException ignored) {
      // Paper Adventure TextDisplay text unavailable
    }
    HAS_ADVENTURE_TEXT_DISPLAY = advText;

    Constructor<PlayerJoinEvent> joinCtor = null;
    try {
      joinCtor = PlayerJoinEvent.class.getConstructor(Player.class, Component.class);
    } catch (ReflectiveOperationException ignored) {
      // Legacy join constructor fallback
    }
    ADVENTURE_JOIN_CONSTRUCTOR = joinCtor;

    Constructor<PlayerQuitEvent> quitCtor = null;
    try {
      quitCtor = PlayerQuitEvent.class.getConstructor(Player.class, Component.class);
    } catch (ReflectiveOperationException ignored) {
      // Legacy quit constructor fallback
    }
    ADVENTURE_QUIT_CONSTRUCTOR = quitCtor;
  }

  private BukkitPlatformBridge() {}

  /**
   * Delivers a resource pack to the target player, selecting the most specific available overload
   * across supported server platforms.
   */
  public static void sendResourcePack(
      Player player, String url, byte[] hash, String prompt, boolean required, Logger logger) {
    Objects.requireNonNull(player, "player");
    Objects.requireNonNull(url, "url");
    Objects.requireNonNull(hash, "hash");
    Logger log = logger != null ? logger : LOGGER;

    // 1. Try modern 4-arg overload with prompt and required flag
    try {
      Method modernMethod =
          player
              .getClass()
              .getMethod(
                  "setResourcePack", String.class, byte[].class, String.class, boolean.class);
      modernMethod.invoke(player, url, hash, prompt, required);
      return;
    } catch (ReflectiveOperationException ignored) {
      // Fallback to legacy 2-arg overload
    }

    // 2. Try legacy 2-arg overload
    try {
      Method legacyMethod =
          player.getClass().getMethod("setResourcePack", String.class, byte[].class);
      legacyMethod.invoke(player, url, hash);
    } catch (ReflectiveOperationException exception) {
      log.log(
          Level.WARNING,
          "Failed to dispatch resource pack to player " + player.getName() + " on this platform",
          exception);
    }
  }

  /**
   * Kicks a player using modern Adventure {@code kick(Component)} when available, falling back
   * cleanly to {@code kickPlayer(String)} on legacy Spigot.
   */
  public static void kickPlayer(
      Player player, String legacyMessage, Component adventureMessage, Logger logger) {
    Objects.requireNonNull(player, "player");
    Logger log = logger != null ? logger : LOGGER;

    if (HAS_ADVENTURE_KICK && adventureMessage != null) {
      try {
        Method kickMethod = player.getClass().getMethod("kick", Component.class);
        kickMethod.invoke(player, adventureMessage);
        return;
      } catch (ReflectiveOperationException ignored) {
        // Fallback to legacy kickPlayer
      }
    }

    try {
      Method legacyKick = player.getClass().getMethod("kickPlayer", String.class);
      legacyKick.invoke(player, legacyMessage != null ? legacyMessage : "");
    } catch (ReflectiveOperationException exception) {
      log.log(
          Level.WARNING,
          "Failed to kick player " + player.getName() + " on this platform",
          exception);
    }
  }

  /**
   * Sets the custom name on an entity using Adventure Component when supported, or legacy String.
   */
  public static void setCustomName(
      Entity entity, String legacyName, Component adventureName, Logger logger) {
    Objects.requireNonNull(entity, "entity");
    Logger log = logger != null ? logger : LOGGER;

    if (HAS_ADVENTURE_CUSTOM_NAME && adventureName != null) {
      try {
        Method method = entity.getClass().getMethod("customName", Component.class);
        method.invoke(entity, adventureName);
        return;
      } catch (ReflectiveOperationException ignored) {
        // Fallback to legacy setCustomName
      }
    }

    try {
      Method method = entity.getClass().getMethod("setCustomName", String.class);
      method.invoke(entity, legacyName != null ? legacyName : "");
    } catch (ReflectiveOperationException exception) {
      log.log(
          Level.WARNING,
          "Failed to set custom name on entity " + entity.getUniqueId() + " on this platform",
          exception);
    }
  }

  /**
   * Creates an inventory with a title, using Adventure Component when supported, or legacy String.
   */
  public static Inventory createInventory(
      InventoryHolder holder, int size, String legacyTitle, Component adventureTitle) {
    if (HAS_ADVENTURE_INVENTORY && adventureTitle != null) {
      try {
        Method method =
            Bukkit.class.getMethod(
                "createInventory", InventoryHolder.class, int.class, Component.class);
        return (Inventory) method.invoke(null, holder, size, adventureTitle);
      } catch (ReflectiveOperationException ignored) {
        // Fallback to legacy createInventory
      }
    }

    try {
      Method method =
          Bukkit.class.getMethod("createInventory", InventoryHolder.class, int.class, String.class);
      return (Inventory) method.invoke(null, holder, size, legacyTitle != null ? legacyTitle : "");
    } catch (ReflectiveOperationException exception) {
      throw new IllegalStateException(
          "Failed to create Bukkit inventory on this platform", exception);
    }
  }

  /** Configures text content on a {@link TextDisplay} entity using Adventure or legacy string. */
  public static void setText(
      TextDisplay display, String legacyText, Component adventureText, Logger logger) {
    Objects.requireNonNull(display, "display");
    Logger log = logger != null ? logger : LOGGER;

    Component comp =
        adventureText != null
            ? adventureText
            : (legacyText != null
                ? LegacyComponentSerializer.legacySection().deserialize(legacyText)
                : Component.empty());

    if (HAS_ADVENTURE_TEXT_DISPLAY) {
      try {
        Method textMethod = display.getClass().getMethod("text", Component.class);
        textMethod.invoke(display, comp);
        return;
      } catch (ReflectiveOperationException ignored) {
        // Fallback to legacy setText
      }
    }

    try {
      Method setTextMethod = display.getClass().getMethod("setText", String.class);
      setTextMethod.invoke(display, legacyText != null ? legacyText : "");
    } catch (ReflectiveOperationException exception) {
      log.log(
          Level.WARNING,
          "Failed to set text on TextDisplay " + display.getUniqueId() + " on this platform",
          exception);
    }
  }

  /**
   * Creates a {@link PlayerJoinEvent} safely in test or runtime contexts, selecting modern or
   * legacy constructors.
   */
  public static PlayerJoinEvent createPlayerJoinEvent(Player player) {
    Objects.requireNonNull(player, "player");
    if (ADVENTURE_JOIN_CONSTRUCTOR != null) {
      try {
        return ADVENTURE_JOIN_CONSTRUCTOR.newInstance(player, Component.text("joined"));
      } catch (ReflectiveOperationException ignored) {
        // Fallback to legacy constructor
      }
    }
    try {
      Constructor<PlayerJoinEvent> ctor =
          PlayerJoinEvent.class.getConstructor(Player.class, String.class);
      return ctor.newInstance(player, "joined");
    } catch (ReflectiveOperationException exception) {
      throw new LinkageError("Failed to instantiate PlayerJoinEvent on this platform", exception);
    }
  }

  /**
   * Creates a {@link PlayerQuitEvent} safely in test or runtime contexts, selecting modern or
   * legacy constructors.
   */
  public static PlayerQuitEvent createPlayerQuitEvent(Player player) {
    Objects.requireNonNull(player, "player");
    if (ADVENTURE_QUIT_CONSTRUCTOR != null) {
      try {
        return ADVENTURE_QUIT_CONSTRUCTOR.newInstance(player, Component.text("left"));
      } catch (ReflectiveOperationException ignored) {
        // Fallback to legacy constructor
      }
    }
    try {
      Constructor<PlayerQuitEvent> ctor =
          PlayerQuitEvent.class.getConstructor(Player.class, String.class);
      return ctor.newInstance(player, "left");
    } catch (ReflectiveOperationException exception) {
      throw new LinkageError("Failed to instantiate PlayerQuitEvent on this platform", exception);
    }
  }

  /** Reflectively instantiates {@link JavaPluginLoader} in test contexts. */
  public static JavaPluginLoader createJavaPluginLoader(Server server) {
    Objects.requireNonNull(server, "server");
    try {
      Constructor<JavaPluginLoader> ctor = JavaPluginLoader.class.getConstructor(Server.class);
      return ctor.newInstance(server);
    } catch (ReflectiveOperationException exception) {
      throw new LinkageError("Failed to reflectively instantiate JavaPluginLoader", exception);
    }
  }

  /** Sets the display name on an {@link ItemMeta} using Adventure or legacy string. */
  public static void setItemDisplayName(ItemMeta meta, Component name) {
    Objects.requireNonNull(meta, "meta");
    if (name == null) {
      return;
    }
    if (HAS_ADVENTURE_ITEM_META) {
      try {
        Method method = meta.getClass().getMethod("displayName", Component.class);
        method.invoke(meta, name);
        return;
      } catch (ReflectiveOperationException ignored) {
        // Fallback below
      }
    }
    try {
      Method legacyMethod = meta.getClass().getMethod("setDisplayName", String.class);
      legacyMethod.invoke(meta, LegacyComponentSerializer.legacySection().serialize(name));
    } catch (ReflectiveOperationException ignored) {
      // Ignore
    }
  }

  /** Sets the lore on an {@link ItemMeta} using Adventure or legacy strings. */
  public static void setItemLore(ItemMeta meta, List<Component> lore) {
    Objects.requireNonNull(meta, "meta");
    if (lore == null || lore.isEmpty()) {
      return;
    }
    if (HAS_ADVENTURE_ITEM_META) {
      try {
        Method method = meta.getClass().getMethod("lore", List.class);
        method.invoke(meta, lore);
        return;
      } catch (ReflectiveOperationException ignored) {
        // Fallback below
      }
    }
    try {
      List<String> legacyLore =
          lore.stream().map(c -> LegacyComponentSerializer.legacySection().serialize(c)).toList();
      Method legacyMethod = meta.getClass().getMethod("setLore", List.class);
      legacyMethod.invoke(meta, legacyLore);
    } catch (ReflectiveOperationException ignored) {
      // Ignore
    }
  }

  /** Sets the custom name on an {@link Entity} using Adventure or legacy string. */
  public static void setEntityCustomName(Entity entity, Component name) {
    Objects.requireNonNull(entity, "entity");
    if (HAS_ADVENTURE_CUSTOM_NAME) {
      try {
        Method method = entity.getClass().getMethod("customName", Component.class);
        method.invoke(entity, name);
        return;
      } catch (ReflectiveOperationException ignored) {
        // Fallback below
      }
    }
    try {
      Method method = entity.getClass().getMethod("setCustomName", String.class);
      method.invoke(
          entity, name != null ? LegacyComponentSerializer.legacySection().serialize(name) : null);
    } catch (ReflectiveOperationException ignored) {
      // Ignore
    }
  }

  /** Registers a new scoreboard objective safely across Paper and Spigot. */
  public static Objective registerNewObjective(
      Scoreboard scoreboard,
      String name,
      String criteriaName,
      String legacyDisplayName,
      Component adventureDisplayName) {
    Objects.requireNonNull(scoreboard, "scoreboard");
    Objects.requireNonNull(name, "name");
    if (adventureDisplayName != null) {
      try {
        Class<?> criteriaClass = Class.forName("org.bukkit.scoreboard.Criteria");
        Field dummyField = criteriaClass.getField("DUMMY");
        Object dummyCriteria = dummyField.get(null);
        Method modernMethod =
            scoreboard
                .getClass()
                .getMethod("registerNewObjective", String.class, criteriaClass, Component.class);
        return (Objective)
            modernMethod.invoke(scoreboard, name, dummyCriteria, adventureDisplayName);
      } catch (ReflectiveOperationException ignored) {
        // Fallback below
      }
      try {
        Method modernStringCriteria =
            scoreboard
                .getClass()
                .getMethod("registerNewObjective", String.class, String.class, Component.class);
        return (Objective)
            modernStringCriteria.invoke(
                scoreboard,
                name,
                criteriaName != null ? criteriaName : "dummy",
                adventureDisplayName);
      } catch (ReflectiveOperationException ignored) {
        // Fallback below
      }
    }
    try {
      Method legacyMethod =
          scoreboard
              .getClass()
              .getMethod("registerNewObjective", String.class, String.class, String.class);
      return (Objective)
          legacyMethod.invoke(
              scoreboard,
              name,
              criteriaName != null ? criteriaName : "dummy",
              legacyDisplayName != null ? legacyDisplayName : name);
    } catch (ReflectiveOperationException exception) {
      try {
        Class<?> criteriaClass = Class.forName("org.bukkit.scoreboard.Criteria");
        Field dummyField = criteriaClass.getField("DUMMY");
        Object dummyCriteria = dummyField.get(null);
        Method legacyCriteriaMethod =
            scoreboard
                .getClass()
                .getMethod("registerNewObjective", String.class, criteriaClass, String.class);
        return (Objective)
            legacyCriteriaMethod.invoke(
                scoreboard,
                name,
                dummyCriteria,
                legacyDisplayName != null ? legacyDisplayName : name);
      } catch (ReflectiveOperationException innerEx) {
        throw new IllegalStateException(
            "Failed to register scoreboard objective on this platform", innerEx);
      }
    }
  }

  /** Creates an {@link EntityDamageByEntityEvent} safely across Paper and Spigot versions. */
  public static EntityDamageByEntityEvent createEntityDamageByEntityEvent(
      Entity damager, Entity victim, DamageSource damageSource, double damage) {
    Objects.requireNonNull(damager, "damager");
    Objects.requireNonNull(victim, "victim");
    try {
      Constructor<EntityDamageByEntityEvent> ctor =
          EntityDamageByEntityEvent.class.getConstructor(
              Entity.class, Entity.class, DamageCause.class, DamageSource.class, double.class);
      return ctor.newInstance(damager, victim, DamageCause.ENTITY_ATTACK, damageSource, damage);
    } catch (ReflectiveOperationException exception) {
      throw new LinkageError(
          "Failed to reflectively instantiate EntityDamageByEntityEvent", exception);
    }
  }
}
