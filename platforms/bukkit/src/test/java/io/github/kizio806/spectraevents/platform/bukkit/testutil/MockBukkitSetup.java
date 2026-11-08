package io.github.kizio806.spectraevents.platform.bukkit.testutil;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.logging.Logger;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemFactory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.plugin.PluginManager;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Score;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.ScoreboardManager;

/**
 * Lightweight, zero-dependency Bukkit server mock for platform testing. Sets up a realistic proxy
 * Server supporting Inventory, ItemFactory, ItemMeta, PluginManager, and Scheduler without
 * byte-buddy or Mockito baggage.
 */
public final class MockBukkitSetup {

  private static final AtomicBoolean INITIALIZED = new AtomicBoolean(false);
  private static final Map<UUID, Player> ONLINE_PLAYERS = new ConcurrentHashMap<>();
  private static final Map<String, org.bukkit.plugin.Plugin> MOCK_PLUGINS =
      new ConcurrentHashMap<>();
  private static final AtomicReference<PluginManager> CUSTOM_PLUGIN_MANAGER =
      new AtomicReference<>();
  private static final AtomicReference<BukkitScheduler> CUSTOM_SCHEDULER = new AtomicReference<>();
  private static final AtomicReference<ScoreboardManager> CUSTOM_SCOREBOARD_MANAGER =
      new AtomicReference<>();
  private static final AtomicBoolean NULL_SCOREBOARD_MANAGER = new AtomicBoolean(false);
  private static final AtomicReference<List<World>> CUSTOM_WORLDS = new AtomicReference<>();
  private static final AtomicReference<String> CUSTOM_BUKKIT_VERSION = new AtomicReference<>();
  private static final AtomicBoolean PRIMARY_THREAD = new AtomicBoolean(true);
  private static final List<Listener> REGISTERED_LISTENERS =
      new java.util.concurrent.CopyOnWriteArrayList<>();
  private static final Map<String, org.bukkit.command.PluginCommand> COMMANDS =
      new ConcurrentHashMap<>();

  private static final Object INIT_LOCK = new Object();
  private static final Object IDENTITY_LOCK = new Object();
  private static final IdentityHashMap<Object, Boolean> IDENTITY_HOLDER = new IdentityHashMap<>();

  private MockBukkitSetup() {}

  private static boolean isSameInstance(Object a, Object b) {
    if (a == null || b == null) {
      return false;
    }
    synchronized (IDENTITY_LOCK) {
      IDENTITY_HOLDER.put(a, Boolean.TRUE);
      boolean same = IDENTITY_HOLDER.containsKey(b);
      IDENTITY_HOLDER.clear();
      return same;
    }
  }

  public static org.bukkit.command.PluginCommand createMockCommand(
      String name, org.bukkit.plugin.Plugin owner) {
    org.bukkit.command.PluginCommand cmd =
        org.bukkit.command.MockPluginCommandFactory.create(name, owner);
    COMMANDS.put(name.toLowerCase(java.util.Locale.ROOT), cmd);
    return cmd;
  }

  public static void setCustomPluginManager(PluginManager pm) {
    CUSTOM_PLUGIN_MANAGER.set(pm);
  }

  public static void setCustomWorlds(List<World> customWorlds) {
    CUSTOM_WORLDS.set(customWorlds);
  }

  public static void setCustomScheduler(BukkitScheduler scheduler) {
    CUSTOM_SCHEDULER.set(scheduler);
  }

  public static void setCustomScoreboardManager(ScoreboardManager manager) {
    CUSTOM_SCOREBOARD_MANAGER.set(manager);
  }

  public static void setNullScoreboardManager(boolean nullScoreboardManager) {
    NULL_SCOREBOARD_MANAGER.set(nullScoreboardManager);
  }

  public static void setCustomBukkitVersion(String bukkitVersion) {
    CUSTOM_BUKKIT_VERSION.set(bukkitVersion);
  }

  public static void resetCustomDelegates() {
    CUSTOM_PLUGIN_MANAGER.set(null);
    CUSTOM_SCHEDULER.set(null);
    CUSTOM_SCOREBOARD_MANAGER.set(null);
    NULL_SCOREBOARD_MANAGER.set(false);
    CUSTOM_WORLDS.set(null);
    CUSTOM_BUKKIT_VERSION.set(null);
    REGISTERED_LISTENERS.clear();
    PRIMARY_THREAD.set(true);
  }

  public static List<Listener> getRegisteredListeners() {
    return List.copyOf(REGISTERED_LISTENERS);
  }

  public static void clearRegisteredListeners() {
    REGISTERED_LISTENERS.clear();
  }

  public static void setPrimaryThread(boolean primary) {
    PRIMARY_THREAD.set(primary);
  }

  public static void registerPlayer(Player player) {
    if (player != null) {
      ONLINE_PLAYERS.put(player.getUniqueId(), player);
    }
  }

  public static void unregisterPlayer(UUID playerId) {
    if (playerId != null) {
      ONLINE_PLAYERS.remove(playerId);
    }
  }

  public static void clearPlayers() {
    ONLINE_PLAYERS.clear();
  }

  public static void registerPlugin(String name, org.bukkit.plugin.Plugin plugin) {
    if (name != null && plugin != null) {
      MOCK_PLUGINS.put(name, plugin);
    }
  }

  public static void unregisterPlugin(String name) {
    if (name != null) {
      MOCK_PLUGINS.remove(name);
    }
  }

  public static void clearPlugins() {
    MOCK_PLUGINS.clear();
  }

  public static org.bukkit.plugin.Plugin createMockPlugin(String name, boolean enabled) {
    return (org.bukkit.plugin.Plugin)
        Proxy.newProxyInstance(
            org.bukkit.plugin.Plugin.class.getClassLoader(),
            new Class<?>[] {org.bukkit.plugin.Plugin.class},
            (proxy, method, args) -> {
              return switch (method.getName()) {
                case "getName" -> name;
                case "isEnabled" -> enabled;
                case "getDescription" ->
                    new org.bukkit.plugin.PluginDescriptionFile(
                        name, "1.0.0", "io.github.test." + name);
                case "getLogger" -> Logger.getLogger(name);
                default -> null;
              };
            });
  }

  public static void ensureInitialized() {
    synchronized (INIT_LOCK) {
      if (Bukkit.getServer() != null) {
        return;
      }

      ItemFactory itemFactory = createMockItemFactory();
      PluginManager pluginManager = createMockPluginManager();
      BukkitScheduler scheduler = createMockScheduler();
      ConsoleCommandSender consoleSender = createMockConsoleSender();
      ScoreboardManager scoreboardManager = createMockScoreboardManager();

      Map<String, World> worlds = new ConcurrentHashMap<>();
      World defaultWorld = createMockWorld("world");
      worlds.put("world", defaultWorld);

      Server server =
          (Server)
              Proxy.newProxyInstance(
                  Server.class.getClassLoader(),
                  new Class<?>[] {Server.class},
                  new InvocationHandler() {
                    @Override
                    public Object invoke(Object proxy, Method method, Object[] args) {
                      return switch (method.getName()) {
                        case "getItemFactory" -> itemFactory;
                        case "getUnsafe" -> {
                          Class<?> unsafeClass = method.getReturnType();
                          yield Proxy.newProxyInstance(
                              unsafeClass.getClassLoader(),
                              new Class<?>[] {unsafeClass},
                              (p, m, a) -> {
                                if (m.getReturnType().isInterface()) {
                                  return Proxy.newProxyInstance(
                                      m.getReturnType().getClassLoader(),
                                      new Class<?>[] {m.getReturnType()},
                                      (p2, m2, a2) -> null);
                                }
                                return null;
                              });
                        }
                        case "getPluginManager" ->
                            CUSTOM_PLUGIN_MANAGER.get() != null
                                ? CUSTOM_PLUGIN_MANAGER.get()
                                : pluginManager;
                        case "getScheduler" ->
                            CUSTOM_SCHEDULER.get() != null ? CUSTOM_SCHEDULER.get() : scheduler;
                        case "getConsoleSender" -> consoleSender;
                        case "getScoreboardManager" ->
                            NULL_SCOREBOARD_MANAGER.get()
                                ? null
                                : (CUSTOM_SCOREBOARD_MANAGER.get() != null
                                    ? CUSTOM_SCOREBOARD_MANAGER.get()
                                    : scoreboardManager);
                        case "getLogger" -> Logger.getLogger("MockServer");
                        case "getPluginCommand" -> {
                          if (args != null && args.length > 0 && args[0] != null) {
                            yield COMMANDS.get(
                                String.valueOf(args[0]).toLowerCase(java.util.Locale.ROOT));
                          }
                          yield null;
                        }
                        case "getName" -> "MockServer";
                        case "getVersion" -> "26.1.0";
                        case "getMinecraftVersion" -> "26.1.0";
                        case "getBukkitVersion" ->
                            CUSTOM_BUKKIT_VERSION.get() != null
                                ? CUSTOM_BUKKIT_VERSION.get()
                                : "26.1.0-R0.1-SNAPSHOT";
                        case "getOnlinePlayers" -> List.copyOf(ONLINE_PLAYERS.values());
                        case "getPlayer" -> {
                          if (args == null || args.length == 0 || args[0] == null) {
                            yield null;
                          }
                          if (args[0] instanceof UUID uuid) {
                            yield ONLINE_PLAYERS.get(uuid);
                          }
                          if (args[0] instanceof String name) {
                            yield ONLINE_PLAYERS.values().stream()
                                .filter(p -> name.equalsIgnoreCase(p.getName()))
                                .findFirst()
                                .orElse(null);
                          }
                          yield null;
                        }
                        case "getWorlds" ->
                            CUSTOM_WORLDS.get() != null
                                ? CUSTOM_WORLDS.get()
                                : List.copyOf(worlds.values());
                        case "getWorld" ->
                            args != null && args.length > 0 && args[0] != null
                                ? worlds.computeIfAbsent(
                                    String.valueOf(args[0]), MockBukkitSetup::createMockWorld)
                                : null;
                        case "getEntity" -> {
                          if (args == null
                              || args.length == 0
                              || !(args[0] instanceof UUID targetUuid)) {
                            yield null;
                          }
                          Player p = ONLINE_PLAYERS.get(targetUuid);
                          if (p != null) {
                            yield p;
                          }
                          yield worlds.values().stream()
                              .flatMap(w -> w.getEntities().stream())
                              .filter(e -> targetUuid.equals(e.getUniqueId()))
                              .findFirst()
                              .orElse(null);
                        }
                        case "getAsyncScheduler",
                            "getRegionScheduler",
                            "getGlobalRegionScheduler" -> {
                          Class<?> returnType = method.getReturnType();
                          if (returnType.isInterface()) {
                            yield Proxy.newProxyInstance(
                                returnType.getClassLoader(),
                                new Class<?>[] {returnType},
                                (rp, rm, ra) -> {
                                  if ("execute".equals(rm.getName()) && ra != null) {
                                    for (Object arg : ra) {
                                      if (arg instanceof Runnable r) {
                                        r.run();
                                        break;
                                      }
                                    }
                                    return null;
                                  }
                                  if (("run".equals(rm.getName())
                                          || "runDelayed".equals(rm.getName())
                                          || "runAtFixedRate".equals(rm.getName()))
                                      && ra != null) {
                                    Class<?> taskClass = rm.getReturnType();
                                    AtomicBoolean cancelled = new AtomicBoolean(false);
                                    Object mockTask =
                                        Proxy.newProxyInstance(
                                            taskClass.getClassLoader(),
                                            new Class<?>[] {taskClass},
                                            (tp, tm, ta) -> {
                                              if ("cancel".equals(tm.getName())) {
                                                cancelled.set(true);
                                                return null;
                                              }
                                              if ("isCancelled".equals(tm.getName())) {
                                                return cancelled.get();
                                              }
                                              if ("getExecutionState".equals(tm.getName())) {
                                                try {
                                                  Class<?> enumCls =
                                                      Class.forName(
                                                          "io.papermc.paper.threadedregions.scheduler.ScheduledTask$ExecutionState");
                                                  Method valueOfMethod =
                                                      enumCls.getMethod("valueOf", String.class);
                                                  return valueOfMethod.invoke(null, "FINISHED");
                                                } catch (ReflectiveOperationException e) {
                                                  java.util.logging.Logger.getLogger(
                                                          "MockBukkitSetup")
                                                      .log(
                                                          java.util.logging.Level.FINEST,
                                                          "ExecutionState lookup failed",
                                                          e);
                                                  return null;
                                                }
                                              }
                                              return null;
                                            });
                                    for (Object arg : ra) {
                                      if (arg != null
                                          && Consumer.class.isAssignableFrom(arg.getClass())) {
                                        try {
                                          Method acceptMethod =
                                              Consumer.class.getMethod("accept", Object.class);
                                          acceptMethod.invoke(arg, mockTask);
                                        } catch (ReflectiveOperationException e) {
                                          java.util.logging.Logger.getLogger("MockBukkitSetup")
                                              .log(
                                                  java.util.logging.Level.FINEST,
                                                  "Consumer dispatch failed",
                                                  e);
                                        }
                                        break;
                                      }
                                    }
                                    return mockTask;
                                  }
                                  if (boolean.class.equals(rm.getReturnType())
                                      || Boolean.class.equals(rm.getReturnType())) {
                                    return true;
                                  }
                                  return null;
                                });
                          }
                          yield null;
                        }
                        case "getRegistry" ->
                            (org.bukkit.Registry<?>)
                                Proxy.newProxyInstance(
                                    org.bukkit.Registry.class.getClassLoader(),
                                    new Class<?>[] {org.bukkit.Registry.class},
                                    (p, m, a) -> {
                                      if ("iterator".equals(m.getName()))
                                        return Collections.emptyIterator();
                                      if ("stream".equals(m.getName()))
                                        return java.util.stream.Stream.empty();
                                      return null;
                                    });
                        case "createInventory" -> {
                          InventoryHolder holder =
                              args != null && args.length > 0 ? (InventoryHolder) args[0] : null;
                          int size =
                              args != null && args.length > 1 && args[1] instanceof Integer sz
                                  ? sz
                                  : 27;
                          String title =
                              args != null && args.length > 2 && args[2] instanceof String t
                                  ? t
                                  : "Inventory";
                          yield createMockInventory(holder, size, title);
                        }
                        case "createBossBar" -> {
                          String title =
                              args != null && args.length > 0 && args[0] instanceof String t
                                  ? t
                                  : "";
                          BarColor color =
                              args != null && args.length > 1 && args[1] instanceof BarColor c
                                  ? c
                                  : BarColor.PURPLE;
                          BarStyle style =
                              args != null && args.length > 2 && args[2] instanceof BarStyle s
                                  ? s
                                  : BarStyle.SOLID;
                          yield createMockBossBar(title, color, style);
                        }
                        case "isPrimaryThread" -> PRIMARY_THREAD.get();
                        case "broadcast" -> 0;
                        default -> null;
                      };
                    }
                  });

      try {
        Bukkit.setServer(server);
        INITIALIZED.set(true);
      } catch (Exception ignored) {
        INITIALIZED.set(true);
      }
    }
  }

  public static Inventory createMockInventory(InventoryHolder holder, int size, String title) {
    ItemStack[] contents = new ItemStack[size];
    return (Inventory)
        Proxy.newProxyInstance(
            Inventory.class.getClassLoader(),
            new Class<?>[] {Inventory.class},
            (proxy, method, args) -> {
              return switch (method.getName()) {
                case "getSize" -> size;
                case "getHolder" -> holder;
                case "getContents", "getStorageContents" -> contents.clone();
                case "getItem" -> {
                  int slot = (Integer) args[0];
                  yield (slot >= 0 && slot < size) ? contents[slot] : null;
                }
                case "setItem" -> {
                  int slot = (Integer) args[0];
                  ItemStack item = (ItemStack) args[1];
                  if (slot >= 0 && slot < size) {
                    contents[slot] = item;
                  }
                  yield null;
                }
                case "setContents" -> {
                  ItemStack[] newContents = (ItemStack[]) args[0];
                  for (int i = 0; i < size; i++) {
                    contents[i] = i < newContents.length ? newContents[i] : null;
                  }
                  yield null;
                }
                case "clear" -> {
                  Arrays.fill(contents, null);
                  yield null;
                }
                case "equals" ->
                    isSameInstance(proxy, args != null && args.length > 0 ? args[0] : null);
                case "hashCode" -> System.identityHashCode(proxy);
                case "toString" -> "MockInventory[" + size + "]";
                default -> null;
              };
            });
  }

  private static ItemFactory createMockItemFactory() {
    return (ItemFactory)
        Proxy.newProxyInstance(
            ItemFactory.class.getClassLoader(),
            new Class<?>[] {ItemFactory.class},
            (proxy, method, args) -> {
              if ("getItemMeta".equals(method.getName())) {
                return createMockItemMeta();
              }
              if ("isApplicable".equals(method.getName())) {
                return true;
              }
              if ("equals".equals(method.getName())) {
                return args != null && args.length > 1 && Objects.equals(args[0], args[1]);
              }
              return null;
            });
  }

  private static ItemMeta createMockItemMeta() {
    Map<String, Object> metaData = new HashMap<>();
    List<String> lore = new ArrayList<>();

    return (ItemMeta)
        Proxy.newProxyInstance(
            ItemMeta.class.getClassLoader(),
            new Class<?>[] {ItemMeta.class, org.bukkit.inventory.meta.Damageable.class},
            new InvocationHandler() {
              @Override
              public Object invoke(Object proxy, Method method, Object[] args) {
                return switch (method.getName()) {
                  case "hasDamage" -> false;
                  case "getDamage" -> 0;
                  case "setDamage", "resetDamage" -> null;
                  case "setDisplayName" -> {
                    metaData.put("displayName", args[0]);
                    yield null;
                  }
                  case "getDisplayName" -> (String) metaData.get("displayName");
                  case "hasDisplayName" -> metaData.containsKey("displayName");
                  case "setLore" -> {
                    lore.clear();
                    if (args[0] instanceof List<?> list) {
                      for (Object o : list) {
                        lore.add(String.valueOf(o));
                      }
                    }
                    yield null;
                  }
                  case "getLore" -> new ArrayList<>(lore);
                  case "hasLore" -> !lore.isEmpty();
                  case "addItemFlags" -> null;
                  case "removeItemFlags" -> null;
                  case "setCustomModelData" -> {
                    metaData.put("customModelData", args[0]);
                    yield null;
                  }
                  case "hasCustomModelData" -> metaData.containsKey("customModelData");
                  case "getCustomModelData" -> (Integer) metaData.get("customModelData");
                  case "clone" -> createMockItemMeta();
                  default -> null;
                };
              }
            });
  }

  private static PluginManager createMockPluginManager() {
    return (PluginManager)
        Proxy.newProxyInstance(
            PluginManager.class.getClassLoader(),
            new Class<?>[] {PluginManager.class},
            (proxy, method, args) -> {
              if ("isPluginEnabled".equals(method.getName())) {
                if (args != null && args.length > 0 && args[0] instanceof String n) {
                  org.bukkit.plugin.Plugin p = MOCK_PLUGINS.get(n);
                  return p != null && p.isEnabled();
                }
                if (args != null
                    && args.length > 0
                    && args[0] instanceof org.bukkit.plugin.Plugin p) {
                  return p.isEnabled();
                }
                return false;
              }
              if ("getPlugin".equals(method.getName())) {
                if (args != null && args.length > 0 && args[0] instanceof String n) {
                  return MOCK_PLUGINS.get(n);
                }
                return null;
              }
              if ("registerEvents".equals(method.getName())
                  && args != null
                  && args.length > 0
                  && args[0] instanceof Listener l) {
                REGISTERED_LISTENERS.add(l);
                return null;
              }
              return null;
            });
  }

  private static BukkitScheduler createMockScheduler() {
    return (BukkitScheduler)
        Proxy.newProxyInstance(
            BukkitScheduler.class.getClassLoader(),
            new Class<?>[] {BukkitScheduler.class},
            (proxy, method, args) -> {
              return switch (method.getName()) {
                case "runTask" -> {
                  if (args != null && args.length > 1 && args[1] instanceof Runnable r) {
                    r.run();
                  }
                  yield createMockBukkitTask();
                }
                case "runTaskLater", "runTaskLaterAsynchronously" -> {
                  yield createMockBukkitTask();
                }
                case "runTaskTimer", "runTaskTimerAsynchronously" -> {
                  yield createMockBukkitTask();
                }
                default -> null;
              };
            });
  }

  public static BukkitTask createMockBukkitTask() {
    AtomicBoolean cancelled = new AtomicBoolean(false);
    return (BukkitTask)
        Proxy.newProxyInstance(
            BukkitTask.class.getClassLoader(),
            new Class<?>[] {BukkitTask.class},
            (proxy, method, args) -> {
              return switch (method.getName()) {
                case "cancel" -> {
                  cancelled.set(true);
                  yield null;
                }
                case "isCancelled" -> cancelled.get();
                case "getTaskId" -> 1;
                default -> null;
              };
            });
  }

  public static ConsoleCommandSender createMockConsoleSender() {
    return (ConsoleCommandSender)
        Proxy.newProxyInstance(
            ConsoleCommandSender.class.getClassLoader(),
            new Class<?>[] {ConsoleCommandSender.class},
            (proxy, method, args) -> {
              return switch (method.getName()) {
                case "getName" -> "CONSOLE";
                case "hasPermission" -> true;
                case "isOp" -> true;
                case "isPermissionSet" -> true;
                case "sendMessage" -> null;
                case "equals" ->
                    isSameInstance(proxy, args != null && args.length > 0 ? args[0] : null);
                case "hashCode" -> System.identityHashCode(proxy);
                case "toString" -> "MockConsoleSender";
                default -> null;
              };
            });
  }

  public static BossBar createMockBossBar(
      String initialTitle, BarColor initialColor, BarStyle initialStyle) {
    AtomicReference<String> title = new AtomicReference<>(initialTitle);
    AtomicReference<BarColor> color = new AtomicReference<>(initialColor);
    AtomicReference<BarStyle> style = new AtomicReference<>(initialStyle);
    AtomicReference<Double> progress = new AtomicReference<>(1.0);
    AtomicBoolean visible = new AtomicBoolean(true);
    Set<Player> players = ConcurrentHashMap.newKeySet();

    return (BossBar)
        Proxy.newProxyInstance(
            BossBar.class.getClassLoader(),
            new Class<?>[] {BossBar.class},
            (proxy, method, args) -> {
              return switch (method.getName()) {
                case "getTitle" -> title.get();
                case "setTitle" -> {
                  title.set(args != null && args.length > 0 ? (String) args[0] : "");
                  yield null;
                }
                case "getColor" -> color.get();
                case "setColor" -> {
                  if (args != null && args.length > 0 && args[0] instanceof BarColor c) {
                    color.set(c);
                  }
                  yield null;
                }
                case "getStyle" -> style.get();
                case "setStyle" -> {
                  if (args != null && args.length > 0 && args[0] instanceof BarStyle s) {
                    style.set(s);
                  }
                  yield null;
                }
                case "getProgress" -> progress.get();
                case "setProgress" -> {
                  if (args != null && args.length > 0 && args[0] instanceof Number n) {
                    progress.set(n.doubleValue());
                  }
                  yield null;
                }
                case "addPlayer" -> {
                  if (args != null && args.length > 0 && args[0] instanceof Player p) {
                    players.add(p);
                  }
                  yield null;
                }
                case "removePlayer" -> {
                  if (args != null && args.length > 0 && args[0] instanceof Player p) {
                    players.remove(p);
                  }
                  yield null;
                }
                case "removeAll" -> {
                  players.clear();
                  yield null;
                }
                case "getPlayers" -> List.copyOf(players);
                case "isVisible" -> visible.get();
                case "setVisible" -> {
                  if (args != null && args.length > 0 && args[0] instanceof Boolean b) {
                    visible.set(b);
                  }
                  yield null;
                }
                case "equals" ->
                    isSameInstance(proxy, args != null && args.length > 0 ? args[0] : null);
                case "hashCode" -> System.identityHashCode(proxy);
                case "toString" -> "MockBossBar[" + title.get() + "]";
                default -> null;
              };
            });
  }

  public static Player createMockPlayer(UUID uuid, String name) {
    return createMockPlayer(uuid, name, true);
  }

  public static Player createMockPlayer(UUID uuid, String name, boolean isOnline) {
    Inventory[] openInv = new Inventory[1];
    World world = createMockWorld("world");
    Location location = new Location(world, 10.0, 64.0, 20.0);
    Player[] playerRef = new Player[1];
    PlayerInventory inventory = createMockPlayerInventory(uuid, name);
    AtomicBoolean online = new AtomicBoolean(isOnline);
    Scoreboard[] playerScoreboard = new Scoreboard[] {createMockScoreboard()};

    Player player =
        (Player)
            Proxy.newProxyInstance(
                Player.class.getClassLoader(),
                new Class<?>[] {Player.class},
                (proxy, method, args) -> {
                  return switch (method.getName()) {
                    case "getUniqueId" -> uuid;
                    case "getName" -> name;
                    case "getWorld" -> world;
                    case "getLocation" -> location;
                    case "getInventory" -> inventory;
                    case "getScoreboard" -> playerScoreboard[0];
                    case "setScoreboard" -> {
                      if (args != null && args.length > 0 && args[0] instanceof Scoreboard sb) {
                        playerScoreboard[0] = sb;
                      }
                      yield null;
                    }
                    case "getOpenInventory" -> {
                      Inventory top = openInv[0] != null ? openInv[0] : inventory;
                      yield (InventoryView)
                          Proxy.newProxyInstance(
                              InventoryView.class.getClassLoader(),
                              new Class<?>[] {InventoryView.class},
                              (vp, vm, va) ->
                                  switch (vm.getName()) {
                                    case "getTopInventory" -> top;
                                    case "getBottomInventory" -> inventory;
                                    case "getPlayer" -> playerRef[0];
                                    default -> null;
                                  });
                    }
                    case "openInventory" -> {
                      if (args != null && args.length > 0 && args[0] instanceof Inventory inv) {
                        openInv[0] = inv;
                      }
                      yield null;
                    }
                    case "closeInventory" -> {
                      openInv[0] = null;
                      yield null;
                    }
                    case "sendMessage" -> null;
                    case "hasPermission" -> true;
                    case "isOnline" -> online.get();
                    case "equals" ->
                        isSameInstance(proxy, args != null && args.length > 0 ? args[0] : null);
                    case "hashCode" -> System.identityHashCode(proxy);
                    case "toString" -> "MockPlayer[" + name + "]";
                    default -> null;
                  };
                });
    playerRef[0] = player;
    return player;
  }

  public static PlayerInventory createMockPlayerInventory(UUID playerId, String playerName) {
    int size = 36;
    ItemStack[] contents = new ItemStack[size];
    return (PlayerInventory)
        Proxy.newProxyInstance(
            PlayerInventory.class.getClassLoader(),
            new Class<?>[] {PlayerInventory.class},
            (proxy, method, args) -> {
              return switch (method.getName()) {
                case "getSize" -> size;
                case "getContents", "getStorageContents" -> contents.clone();
                case "getItem" -> {
                  int slot = (Integer) args[0];
                  yield (slot >= 0 && slot < size) ? contents[slot] : null;
                }
                case "setItem" -> {
                  int slot = (Integer) args[0];
                  ItemStack item = (ItemStack) args[1];
                  if (slot >= 0 && slot < size) {
                    contents[slot] = item;
                  }
                  yield null;
                }
                case "addItem" -> {
                  ItemStack[] toAdd = (ItemStack[]) args[0];
                  Map<Integer, ItemStack> leftovers = new HashMap<>();
                  for (int i = 0; i < toAdd.length; i++) {
                    ItemStack stack = toAdd[i];
                    if (stack == null || stack.getType().isAir()) {
                      continue;
                    }
                    int remaining = stack.getAmount();
                    for (int slot = 0; slot < size && remaining > 0; slot++) {
                      ItemStack existing = contents[slot];
                      if (existing != null && existing.isSimilar(stack)) {
                        int space = existing.getMaxStackSize() - existing.getAmount();
                        int move = Math.min(space, remaining);
                        existing.setAmount(existing.getAmount() + move);
                        remaining -= move;
                      }
                    }
                    for (int slot = 0; slot < size && remaining > 0; slot++) {
                      if (contents[slot] == null || contents[slot].getType().isAir()) {
                        int move = Math.min(stack.getMaxStackSize(), remaining);
                        ItemStack placed = stack.clone();
                        placed.setAmount(move);
                        contents[slot] = placed;
                        remaining -= move;
                      }
                    }
                    if (remaining > 0) {
                      ItemStack leftover = stack.clone();
                      leftover.setAmount(remaining);
                      leftovers.put(i, leftover);
                    }
                  }
                  yield leftovers;
                }
                case "clear" -> {
                  Arrays.fill(contents, null);
                  yield null;
                }
                case "equals" ->
                    isSameInstance(proxy, args != null && args.length > 0 ? args[0] : null);
                case "hashCode" -> System.identityHashCode(proxy);
                case "toString" -> "MockPlayerInventory[" + playerName + "]";
                default -> null;
              };
            });
  }

  public static World createMockWorld(String name) {
    UUID worldId = UUID.randomUUID();
    List<Entity> worldEntities = new ArrayList<>();
    return (World)
        Proxy.newProxyInstance(
            World.class.getClassLoader(),
            new Class<?>[] {World.class},
            (proxy, method, args) -> {
              return switch (method.getName()) {
                case "getName" -> name;
                case "getUID" -> worldId;
                case "getSpawnLocation" -> new Location((World) proxy, 0.0, 64.0, 0.0);
                case "getEntities", "getNearbyEntities" -> List.copyOf(worldEntities);
                case "getLoadedChunks" -> {
                  org.bukkit.Chunk chunk =
                      (org.bukkit.Chunk)
                          Proxy.newProxyInstance(
                              org.bukkit.Chunk.class.getClassLoader(),
                              new Class<?>[] {org.bukkit.Chunk.class},
                              (cp, cm, ca) -> {
                                if ("getEntities".equals(cm.getName())) {
                                  return worldEntities.toArray(new Entity[0]);
                                }
                                if ("getWorld".equals(cm.getName())) {
                                  return proxy;
                                }
                                return null;
                              });
                  yield new org.bukkit.Chunk[] {chunk};
                }
                case "playSound", "spawnParticle" -> null;
                case "spawnEntity" -> {
                  Location loc = (Location) args[0];
                  EntityType entityType = (EntityType) args[1];
                  Entity entity = createMockEntityWithDetails(entityType, loc);
                  worldEntities.add(entity);
                  yield entity;
                }
                case "dropItem", "dropItemNaturally" -> {
                  Location loc = (Location) args[0];
                  ItemStack item = (ItemStack) args[1];
                  Item dropped = createMockItemEntity(item, loc);
                  worldEntities.add(dropped);
                  yield dropped;
                }
                case "spawn" -> {
                  Location loc = (Location) args[0];
                  Class<?> entityClass = (Class<?>) args[1];
                  Entity entity = createMockEntity(entityClass, loc);
                  worldEntities.add(entity);
                  if (args.length > 2 && args[2] != null) {
                    try {
                      Method acceptMethod = Consumer.class.getMethod("accept", Object.class);
                      acceptMethod.invoke(args[2], entity);
                    } catch (ReflectiveOperationException e) {
                      java.util.logging.Logger.getLogger("MockBukkitSetup")
                          .log(java.util.logging.Level.FINEST, "World spawn consumer failed", e);
                    }
                  }
                  yield entity;
                }
                case "equals" ->
                    isSameInstance(proxy, args != null && args.length > 0 ? args[0] : null);
                case "hashCode" -> name.hashCode();
                case "toString" -> "MockWorld[" + name + "]";
                default -> null;
              };
            });
  }

  public static Entity createMockEntity(Class<?> type, Location location) {
    UUID uuid = UUID.randomUUID();
    AtomicBoolean valid = new AtomicBoolean(true);
    PersistentDataContainer pdc = createMockPdc();
    return (Entity)
        Proxy.newProxyInstance(
            type.getClassLoader(),
            new Class<?>[] {type},
            (proxy, method, args) -> {
              return switch (method.getName()) {
                case "getUniqueId" -> uuid;
                case "isValid" -> valid.get();
                case "remove" -> {
                  valid.set(false);
                  yield null;
                }
                case "getLocation" -> location;
                case "getPersistentDataContainer" -> pdc;
                case "getScheduler" -> {
                  Class<?> returnType = method.getReturnType();
                  if (returnType.isInterface()) {
                    yield Proxy.newProxyInstance(
                        returnType.getClassLoader(),
                        new Class<?>[] {returnType},
                        (sp, sm, sa) -> {
                          if ("execute".equals(sm.getName())) {
                            if (sa != null && sa.length > 1 && sa[1] instanceof Runnable r) {
                              r.run();
                            }
                            return true;
                          }
                          if (boolean.class.equals(sm.getReturnType())
                              || Boolean.class.equals(sm.getReturnType())) {
                            return true;
                          }
                          return null;
                        });
                  }
                  yield null;
                }
                case "setInterpolationDuration", "setTransformationMatrix" -> null;
                case "equals" ->
                    isSameInstance(proxy, args != null && args.length > 0 ? args[0] : null);
                case "hashCode" -> uuid.hashCode();
                default -> null;
              };
            });
  }

  public static Entity createMockEntityWithDetails(EntityType entityType, Location location) {
    UUID uuid = UUID.randomUUID();
    AtomicBoolean valid = new AtomicBoolean(true);
    PersistentDataContainer pdc = createMockPdc();
    String[] customName = new String[1];
    boolean[] nameVisible = new boolean[1];
    return (Entity)
        Proxy.newProxyInstance(
            org.bukkit.entity.LivingEntity.class.getClassLoader(),
            new Class<?>[] {org.bukkit.entity.LivingEntity.class},
            (proxy, method, args) -> {
              return switch (method.getName()) {
                case "getUniqueId" -> uuid;
                case "isValid" -> valid.get();
                case "remove" -> {
                  valid.set(false);
                  yield null;
                }
                case "getLocation" -> location;
                case "getType" -> entityType;
                case "getPersistentDataContainer" -> pdc;
                case "setCustomName" -> {
                  customName[0] = (String) args[0];
                  yield null;
                }
                case "getCustomName" -> customName[0];
                case "setCustomNameVisible" -> {
                  nameVisible[0] = (Boolean) args[0];
                  yield null;
                }
                case "isCustomNameVisible" -> nameVisible[0];
                case "equals" ->
                    isSameInstance(proxy, args != null && args.length > 0 ? args[0] : null);
                case "hashCode" -> uuid.hashCode();
                case "toString" -> "MockEntity[" + entityType + "," + uuid + "]";
                default -> null;
              };
            });
  }

  public static Item createMockItemEntity(ItemStack item, Location location) {
    UUID uuid = UUID.randomUUID();
    AtomicBoolean valid = new AtomicBoolean(true);
    PersistentDataContainer pdc = createMockPdc();
    ItemStack[] itemHolder = new ItemStack[] {item != null ? item.clone() : null};
    return (Item)
        Proxy.newProxyInstance(
            Item.class.getClassLoader(),
            new Class<?>[] {Item.class},
            (proxy, method, args) -> {
              return switch (method.getName()) {
                case "getUniqueId" -> uuid;
                case "isValid" -> valid.get();
                case "remove" -> {
                  valid.set(false);
                  yield null;
                }
                case "getLocation" -> location;
                case "getType" -> EntityType.ITEM;
                case "getItemStack" -> itemHolder[0];
                case "setItemStack" -> {
                  itemHolder[0] = (ItemStack) args[0];
                  yield null;
                }
                case "getPersistentDataContainer" -> pdc;
                case "equals" ->
                    isSameInstance(proxy, args != null && args.length > 0 ? args[0] : null);
                case "hashCode" -> uuid.hashCode();
                case "toString" -> "MockItem[" + uuid + "]";
                default -> null;
              };
            });
  }

  public static org.bukkit.persistence.PersistentDataContainer createMockPdc() {
    Map<org.bukkit.NamespacedKey, Object> store = new HashMap<>();
    return (org.bukkit.persistence.PersistentDataContainer)
        Proxy.newProxyInstance(
            org.bukkit.persistence.PersistentDataContainer.class.getClassLoader(),
            new Class<?>[] {org.bukkit.persistence.PersistentDataContainer.class},
            (proxy, method, args) -> {
              return switch (method.getName()) {
                case "set" -> {
                  store.put((org.bukkit.NamespacedKey) args[0], args[2]);
                  yield null;
                }
                case "get" -> store.get(args[0]);
                case "has" -> store.containsKey(args[0]);
                case "remove" -> store.remove(args[0]);
                default -> null;
              };
            });
  }

  public static InventoryClickEvent createClickEvent(
      Player player, Inventory inventory, int rawSlot) {
    return createClickEvent(player, inventory, rawSlot, ClickType.LEFT);
  }

  public static InventoryClickEvent createClickEvent(
      Player player, Inventory inventory, int rawSlot, ClickType clickType) {
    InventoryView view =
        (InventoryView)
            Proxy.newProxyInstance(
                InventoryView.class.getClassLoader(),
                new Class<?>[] {InventoryView.class},
                (proxy, method, args) -> {
                  return switch (method.getName()) {
                    case "getTopInventory" -> inventory;
                    case "getBottomInventory" -> inventory;
                    case "getPlayer" -> player;
                    case "convertSlot" -> rawSlot;
                    default -> null;
                  };
                });
    return new InventoryClickEvent(
        view,
        InventoryType.SlotType.CONTAINER,
        rawSlot,
        clickType,
        clickType == ClickType.RIGHT ? InventoryAction.PICKUP_HALF : InventoryAction.PICKUP_ALL);
  }

  public static ScoreboardManager createMockScoreboardManager() {
    Scoreboard mainBoard = createMockScoreboard();
    return (ScoreboardManager)
        Proxy.newProxyInstance(
            ScoreboardManager.class.getClassLoader(),
            new Class<?>[] {ScoreboardManager.class},
            (proxy, method, args) -> {
              return switch (method.getName()) {
                case "getMainScoreboard" -> mainBoard;
                case "getNewScoreboard" -> createMockScoreboard();
                default -> null;
              };
            });
  }

  public static Scoreboard createMockScoreboard() {
    Map<String, Objective> objectives = new ConcurrentHashMap<>();
    Set<String> entries = ConcurrentHashMap.newKeySet();
    return (Scoreboard)
        Proxy.newProxyInstance(
            Scoreboard.class.getClassLoader(),
            new Class<?>[] {Scoreboard.class},
            (proxy, method, args) -> {
              return switch (method.getName()) {
                case "registerNewObjective" -> {
                  String name = (String) args[0];
                  Objective obj = createMockObjective(name, proxy, entries);
                  objectives.put(name, obj);
                  yield obj;
                }
                case "getObjective" -> {
                  if (args != null && args.length > 0) {
                    if (args[0] instanceof String name) {
                      yield objectives.get(name);
                    }
                    if (args[0] instanceof DisplaySlot) {
                      yield objectives.values().stream().findFirst().orElse(null);
                    }
                  }
                  yield null;
                }
                case "getEntries" -> Set.copyOf(entries);
                case "resetScores" -> {
                  if (args != null && args.length > 0 && args[0] instanceof String e) {
                    entries.remove(e);
                  }
                  yield null;
                }
                case "clearSlot" -> {
                  objectives.clear();
                  yield null;
                }
                case "equals" ->
                    isSameInstance(proxy, args != null && args.length > 0 ? args[0] : null);
                case "hashCode" -> System.identityHashCode(proxy);
                case "toString" ->
                    "MockScoreboard@" + Integer.toHexString(System.identityHashCode(proxy));
                default -> null;
              };
            });
  }

  public static Objective createMockObjective(String name, Object scoreboardProxy) {
    return createMockObjective(name, scoreboardProxy, null);
  }

  public static Objective createMockObjective(
      String name, Object scoreboardProxy, Set<String> entriesHolder) {
    AtomicReference<String> displayName = new AtomicReference<>(name);
    return (Objective)
        Proxy.newProxyInstance(
            Objective.class.getClassLoader(),
            new Class<?>[] {Objective.class},
            (proxy, method, args) -> {
              return switch (method.getName()) {
                case "getName" -> name;
                case "getDisplayName" -> displayName.get();
                case "setDisplayName" -> {
                  displayName.set((String) args[0]);
                  yield null;
                }
                case "setDisplaySlot" -> null;
                case "getDisplaySlot" -> DisplaySlot.SIDEBAR;
                case "getScore" -> {
                  if (args != null && args.length > 0 && args[0] != null) {
                    String entry = String.valueOf(args[0]);
                    if (entriesHolder != null) {
                      entriesHolder.add(entry);
                    }
                    yield createMockScore(entry);
                  }
                  yield createMockScore("");
                }
                case "unregister" -> null;
                case "getScoreboard" -> scoreboardProxy;
                default -> null;
              };
            });
  }

  public static Score createMockScore(String entry) {
    AtomicInteger scoreVal = new AtomicInteger(0);
    return (Score)
        Proxy.newProxyInstance(
            Score.class.getClassLoader(),
            new Class<?>[] {Score.class},
            (proxy, method, args) -> {
              return switch (method.getName()) {
                case "getEntry" -> entry;
                case "getScore" -> scoreVal.get();
                case "setScore" -> {
                  scoreVal.set((Integer) args[0]);
                  yield null;
                }
                default -> null;
              };
            });
  }
}
