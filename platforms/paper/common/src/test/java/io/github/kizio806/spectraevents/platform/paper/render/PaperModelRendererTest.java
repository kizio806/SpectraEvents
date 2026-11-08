package io.github.kizio806.spectraevents.platform.paper.render;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.kizio806.spectraevents.application.model.runtime.ModelAnchor;
import io.github.kizio806.spectraevents.application.model.runtime.ModelRuntimeId;
import io.github.kizio806.spectraevents.application.model.runtime.RenderedModelHandle;
import io.github.kizio806.spectraevents.application.model.runtime.RenderedPartHandle;
import io.github.kizio806.spectraevents.core.event.runtime.EventInstanceId;
import io.github.kizio806.spectraevents.core.visual.model.ModelId;
import io.github.kizio806.spectraevents.core.visual.model.ModelPartId;
import io.github.kizio806.spectraevents.core.visual.model.ModelTransform;
import io.github.kizio806.spectraevents.core.visual.model.TextAssetRef;
import io.github.kizio806.spectraevents.platform.bukkit.testutil.MockBukkitSetup;
import java.lang.reflect.Proxy;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.TextDisplay;
import org.bukkit.plugin.Plugin;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PaperModelRendererTest {

  @BeforeAll
  static void beforeAll() {
    MockBukkitSetup.ensureInitialized();
  }

  private Plugin plugin;
  private PaperModelRenderer renderer;

  @BeforeEach
  void setUp() {
    plugin =
        (Plugin)
            Proxy.newProxyInstance(
                Plugin.class.getClassLoader(),
                new Class<?>[] {Plugin.class},
                (proxy, method, args) -> {
                  if ("getLogger".equals(method.getName())) {
                    return java.util.logging.Logger.getLogger("test");
                  }
                  if ("isEnabled".equals(method.getName())) {
                    return false;
                  }
                  return null;
                });
    renderer = new PaperModelRenderer(plugin);
  }

  @Test
  void testConstructor() {
    assertNotNull(new PaperModelRenderer(plugin));
    assertNotNull(new PaperModelRenderer(plugin, null));
  }

  @Test
  void testConfigureTextDisplay() {
    TextDisplay textDisplay =
        (TextDisplay)
            Proxy.newProxyInstance(
                TextDisplay.class.getClassLoader(),
                new Class<?>[] {TextDisplay.class},
                (proxy, method, args) -> null);

    assertDoesNotThrow(() -> renderer.configureTextDisplay(textDisplay, TextAssetRef.of("")));
    assertDoesNotThrow(() -> renderer.configureTextDisplay(textDisplay, TextAssetRef.of("   ")));
    assertDoesNotThrow(
        () -> renderer.configureTextDisplay(textDisplay, TextAssetRef.of("<red>Hello</red>")));
  }

  @Test
  void testApplyTransformTeleportsAndFailure() {
    ModelRuntimeId runtimeId = ModelRuntimeId.generate();
    EventInstanceId eventId = EventInstanceId.generate();
    RenderedModelHandle handle =
        new RenderedModelHandle(
            runtimeId,
            new ModelId("stone_model"),
            eventId,
            new ModelAnchor("world", 0, 0, 0, 0, 0),
            Collections.emptyMap(),
            Collections.emptyMap());

    Entity successfulEntity =
        (Entity)
            Proxy.newProxyInstance(
                Entity.class.getClassLoader(),
                new Class<?>[] {Entity.class},
                (proxy, method, args) -> {
                  if ("teleportAsync".equals(method.getName())) {
                    return CompletableFuture.completedFuture(true);
                  }
                  return null;
                });

    Entity failingEntity =
        (Entity)
            Proxy.newProxyInstance(
                Entity.class.getClassLoader(),
                new Class<?>[] {Entity.class},
                (proxy, method, args) -> {
                  if ("teleportAsync".equals(method.getName())) {
                    return CompletableFuture.failedFuture(
                        new RuntimeException("Simulated teleport error"));
                  }
                  return null;
                });

    World world = MockBukkitSetup.createMockWorld("world");
    Location newLoc = new Location(world, 10, 64, 10);

    assertDoesNotThrow(
        () ->
            renderer.applyTransformTeleports(
                handle, List.of(successfulEntity, failingEntity), newLoc));
  }

  @Test
  void testUpdatePartTransformValidations() {
    ModelRuntimeId runtimeId = ModelRuntimeId.generate();
    EventInstanceId eventId = EventInstanceId.generate();
    RenderedModelHandle handle =
        new RenderedModelHandle(
            runtimeId,
            new ModelId("stone_model"),
            eventId,
            new ModelAnchor("world", 0, 0, 0, 0, 0),
            Collections.emptyMap(),
            Collections.emptyMap());

    assertThrows(
        NullPointerException.class,
        () ->
            renderer.updatePartTransform(
                null, new ModelPartId("part"), ModelTransform.IDENTITY, 5));
    assertThrows(
        NullPointerException.class,
        () -> renderer.updatePartTransform(handle, null, ModelTransform.IDENTITY, 5));
    assertThrows(
        NullPointerException.class,
        () -> renderer.updatePartTransform(handle, new ModelPartId("part"), null, 5));

    // Non-existent part returns false
    assertFalse(
        renderer.updatePartTransform(
            handle, new ModelPartId("non_existent"), ModelTransform.IDENTITY, 5));

    // Non-existent entity UUID in Bukkit returns false
    ModelPartId missingEntityPart = new ModelPartId("missing_entity");
    RenderedPartHandle missingPartHandle =
        new RenderedPartHandle(missingEntityPart, UUID.randomUUID());
    RenderedModelHandle handleWithMissingEntity =
        new RenderedModelHandle(
            runtimeId,
            new ModelId("stone_model"),
            eventId,
            new ModelAnchor("world", 0, 0, 0, 0, 0),
            Map.of(missingEntityPart, missingPartHandle),
            Collections.emptyMap());
    assertFalse(
        renderer.updatePartTransform(
            handleWithMissingEntity, missingEntityPart, ModelTransform.IDENTITY, 5));
  }

  @Test
  void testUpdatePartTransformWithValidItemDisplay() {
    ModelRuntimeId runtimeId = ModelRuntimeId.generate();
    EventInstanceId eventId = EventInstanceId.generate();
    ModelPartId partId = new ModelPartId("head");

    World world = Bukkit.getWorld("world");
    if (world == null) {
      world = MockBukkitSetup.createMockWorld("world");
    }
    Location loc = new Location(world, 0, 0, 0);
    ItemDisplay itemDisplay = world.spawn(loc, ItemDisplay.class);

    RenderedPartHandle partHandle = new RenderedPartHandle(partId, itemDisplay.getUniqueId());

    RenderedModelHandle handle =
        new RenderedModelHandle(
            runtimeId,
            new ModelId("stone_model"),
            eventId,
            new ModelAnchor("world", 0, 0, 0, 0, 0),
            Map.of(partId, partHandle),
            Collections.emptyMap());

    // Positive interpolation duration
    assertTrue(renderer.updatePartTransform(handle, partId, ModelTransform.IDENTITY, 5));

    // Negative interpolation duration
    assertTrue(renderer.updatePartTransform(handle, partId, ModelTransform.IDENTITY, -1));
  }

  @Test
  void testRemoveModelAndRemoveAll() {
    assertThrows(NullPointerException.class, () -> renderer.removeModel(null));

    ModelRuntimeId runtimeId = ModelRuntimeId.generate();
    EventInstanceId eventId = EventInstanceId.generate();

    // Unknown world
    RenderedModelHandle unknownWorldHandle =
        new RenderedModelHandle(
            runtimeId,
            new ModelId("stone_model"),
            eventId,
            new ModelAnchor("unknown_world_404", 0, 0, 0, 0, 0),
            Collections.emptyMap(),
            Collections.emptyMap());
    assertFalse(renderer.removeModel(unknownWorldHandle));

    // Known world with plugin disabled
    RenderedModelHandle knownHandle =
        new RenderedModelHandle(
            runtimeId,
            new ModelId("stone_model"),
            eventId,
            new ModelAnchor("world", 0, 0, 0, 0, 0),
            Collections.emptyMap(),
            Collections.emptyMap());
    assertFalse(renderer.removeModel(knownHandle));

    // Known world with plugin enabled
    Plugin enabledPlugin =
        (Plugin)
            Proxy.newProxyInstance(
                Plugin.class.getClassLoader(),
                new Class<?>[] {Plugin.class},
                (proxy, method, args) -> {
                  if ("isEnabled".equals(method.getName())) return true;
                  return null;
                });
    PaperModelRenderer enabledRenderer = new PaperModelRenderer(enabledPlugin);
    assertFalse(enabledRenderer.removeModel(knownHandle));

    // removeAll
    assertDoesNotThrow(() -> renderer.removeAll());
    assertDoesNotThrow(() -> enabledRenderer.removeAll());
  }
}
