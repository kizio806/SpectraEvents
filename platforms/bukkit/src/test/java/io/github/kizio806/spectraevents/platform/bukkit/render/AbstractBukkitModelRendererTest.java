package io.github.kizio806.spectraevents.platform.bukkit.render;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.kizio806.spectraevents.application.model.runtime.DiscoveredModelEntity;
import io.github.kizio806.spectraevents.application.model.runtime.ModelAnchor;
import io.github.kizio806.spectraevents.application.model.runtime.ModelRuntimeId;
import io.github.kizio806.spectraevents.application.model.runtime.RenderedModelHandle;
import io.github.kizio806.spectraevents.core.event.runtime.EventInstanceId;
import io.github.kizio806.spectraevents.core.visual.model.BillboardMode;
import io.github.kizio806.spectraevents.core.visual.model.BlockAssetRef;
import io.github.kizio806.spectraevents.core.visual.model.DisplayTransformMode;
import io.github.kizio806.spectraevents.core.visual.model.InteractionDefinition;
import io.github.kizio806.spectraevents.core.visual.model.InteractionId;
import io.github.kizio806.spectraevents.core.visual.model.ItemAssetRef;
import io.github.kizio806.spectraevents.core.visual.model.ModelDefinition;
import io.github.kizio806.spectraevents.core.visual.model.ModelId;
import io.github.kizio806.spectraevents.core.visual.model.ModelPartDefinition;
import io.github.kizio806.spectraevents.core.visual.model.ModelPartId;
import io.github.kizio806.spectraevents.core.visual.model.ModelPartType;
import io.github.kizio806.spectraevents.core.visual.model.ModelRenderProperties;
import io.github.kizio806.spectraevents.core.visual.model.ModelTransform;
import io.github.kizio806.spectraevents.core.visual.model.Quaternion;
import io.github.kizio806.spectraevents.core.visual.model.TextAlignment;
import io.github.kizio806.spectraevents.core.visual.model.TextAssetRef;
import io.github.kizio806.spectraevents.core.visual.model.Vector3;
import io.github.kizio806.spectraevents.platform.bukkit.integration.item.CustomItemProvider;
import io.github.kizio806.spectraevents.platform.bukkit.testutil.MockBukkitSetup;
import java.lang.reflect.Proxy;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.TextDisplay;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.joml.Matrix4f;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AbstractBukkitModelRendererTest {

  private TestModelRenderer renderer;
  private CustomItemProvider itemProvider;
  private Plugin mockPlugin;

  static class TestModelRenderer extends AbstractBukkitModelRenderer {
    final AtomicReference<Location> lastTeleport = new AtomicReference<>();

    TestModelRenderer(Plugin plugin, CustomItemProvider itemProvider) {
      super(plugin, itemProvider);
    }

    @Override
    protected void applyTransformTeleports(
        RenderedModelHandle handle, List<Entity> entities, Location newLoc) {
      this.lastTeleport.set(newLoc);
    }
  }

  @BeforeAll
  static void beforeAll() {
    MockBukkitSetup.ensureInitialized();
  }

  @BeforeEach
  void setUp() {
    mockPlugin =
        (Plugin)
            Proxy.newProxyInstance(
                Plugin.class.getClassLoader(),
                new Class<?>[] {Plugin.class},
                (proxy, method, args) -> null);

    itemProvider =
        new CustomItemProvider() {
          @Override
          public boolean isAvailable() {
            return true;
          }

          @Override
          public ItemStack resolveItem(String itemId, int amount) {
            if ("custom:special_sword".equals(itemId)) {
              return new ItemStack(Material.DIAMOND_SWORD, amount);
            }
            return null;
          }
        };

    renderer = new TestModelRenderer(mockPlugin, itemProvider);
  }

  @Test
  void testTransformMatrixMath() {
    ModelTransform identity = ModelTransform.IDENTITY;
    Matrix4f matrix = renderer.toMatrix4f(identity);
    assertNotNull(matrix);

    ModelTransform complex =
        new ModelTransform(
            new Vector3(1.0f, 2.0f, 3.0f),
            Quaternion.fromEulerDegrees(0.0f, 90.0f, 0.0f),
            new Vector3(2.0f, 2.0f, 2.0f),
            new Vector3(0.5f, 0.5f, 0.5f));
    Matrix4f complexMatrix = renderer.toMatrix4f(complex);
    assertNotNull(complexMatrix);
  }

  @Test
  void testEnumConversions() {
    assertEquals(Display.Billboard.FIXED, renderer.toBillboardMode(BillboardMode.FIXED));
    assertEquals(Display.Billboard.CENTER, renderer.toBillboardMode(BillboardMode.CENTER));
    assertEquals(Display.Billboard.VERTICAL, renderer.toBillboardMode(BillboardMode.VERTICAL));
    assertEquals(Display.Billboard.HORIZONTAL, renderer.toBillboardMode(BillboardMode.HORIZONTAL));

    assertEquals(
        ItemDisplay.ItemDisplayTransform.NONE,
        renderer.toItemDisplayTransform(DisplayTransformMode.NONE));
    assertEquals(
        ItemDisplay.ItemDisplayTransform.HEAD,
        renderer.toItemDisplayTransform(DisplayTransformMode.HEAD));
    assertEquals(
        ItemDisplay.ItemDisplayTransform.GUI,
        renderer.toItemDisplayTransform(DisplayTransformMode.GUI));
    assertEquals(
        ItemDisplay.ItemDisplayTransform.GROUND,
        renderer.toItemDisplayTransform(DisplayTransformMode.GROUND));
    assertEquals(
        ItemDisplay.ItemDisplayTransform.FIXED,
        renderer.toItemDisplayTransform(DisplayTransformMode.FIXED));
    assertEquals(
        ItemDisplay.ItemDisplayTransform.THIRDPERSON_LEFTHAND,
        renderer.toItemDisplayTransform(DisplayTransformMode.THIRDPERSON_LEFTHAND));
    assertEquals(
        ItemDisplay.ItemDisplayTransform.THIRDPERSON_RIGHTHAND,
        renderer.toItemDisplayTransform(DisplayTransformMode.THIRDPERSON_RIGHTHAND));
    assertEquals(
        ItemDisplay.ItemDisplayTransform.FIRSTPERSON_LEFTHAND,
        renderer.toItemDisplayTransform(DisplayTransformMode.FIRSTPERSON_LEFTHAND));
    assertEquals(
        ItemDisplay.ItemDisplayTransform.FIRSTPERSON_RIGHTHAND,
        renderer.toItemDisplayTransform(DisplayTransformMode.FIRSTPERSON_RIGHTHAND));

    assertEquals(TextDisplay.TextAlignment.CENTER, renderer.toTextAlignment(TextAlignment.CENTER));
    assertEquals(TextDisplay.TextAlignment.LEFT, renderer.toTextAlignment(TextAlignment.LEFT));
    assertEquals(TextDisplay.TextAlignment.RIGHT, renderer.toTextAlignment(TextAlignment.RIGHT));
  }

  @Test
  void testResolveItemStack() {
    ItemStack normal = renderer.resolveItemStack("minecraft:iron_ingot");
    assertEquals(Material.IRON_INGOT, normal.getType());

    ItemStack custom = renderer.resolveItemStack("custom:special_sword");
    assertEquals(Material.DIAMOND_SWORD, custom.getType());

    ItemStack fallback = renderer.resolveItemStack("unknown:invalid_nonexistent_xyz");
    assertEquals(Material.MAGMA_BLOCK, fallback.getType());

    ItemStack generated = renderer.resolveItemStack("spectraevents:metin_stone/root");
    assertEquals(Material.PAPER, generated.getType());
  }

  @Test
  void testModelSpawningAndRemoval() {
    ModelId modelId = new ModelId("boss_model");
    ModelPartDefinition itemPart =
        new ModelPartDefinition(
            new ModelPartId("part_sword"),
            null,
            ModelPartType.ITEM_DISPLAY,
            ModelTransform.IDENTITY,
            ModelTransform.IDENTITY,
            ModelRenderProperties.DEFAULT,
            new ItemAssetRef("minecraft:diamond_sword", DisplayTransformMode.HEAD));

    ModelPartDefinition blockPart =
        new ModelPartDefinition(
            new ModelPartId("part_core"),
            null,
            ModelPartType.BLOCK_DISPLAY,
            ModelTransform.IDENTITY,
            ModelTransform.IDENTITY,
            ModelRenderProperties.DEFAULT,
            new BlockAssetRef("minecraft:obsidian"));

    ModelPartDefinition textPart =
        new ModelPartDefinition(
            new ModelPartId("part_label"),
            null,
            ModelPartType.TEXT_DISPLAY,
            ModelTransform.IDENTITY,
            ModelTransform.IDENTITY,
            ModelRenderProperties.DEFAULT,
            new TextAssetRef("Boss Name", TextAlignment.CENTER, 200, 0, 255, true, false));

    InteractionDefinition hitbox =
        new InteractionDefinition(
            new InteractionId("hitbox_main"),
            null,
            new Vector3(0.0f, 1.0f, 0.0f),
            new Vector3(0.0f, 1.0f, 0.0f),
            2.0f,
            3.0f,
            true);

    ModelDefinition def =
        new ModelDefinition(modelId, List.of(itemPart, blockPart, textPart), List.of(hitbox));

    ModelRuntimeId runtimeId = ModelRuntimeId.of("run_001");
    ModelAnchor anchor = new ModelAnchor("world", 10.0, 64.0, 20.0, 0.0f, 0.0f);
    EventInstanceId eventId = EventInstanceId.generate();

    RenderedModelHandle handle = renderer.spawnModel(runtimeId, def, anchor, eventId);
    assertNotNull(handle);
    assertEquals(runtimeId, handle.runtimeId());
    assertEquals(modelId, handle.definitionId());
    assertEquals(eventId, handle.ownerEventId());
    assertEquals(3, handle.parts().size());
    assertEquals(1, handle.interactions().size());

    // Update model transform
    renderer.updateModelTransform(handle, new ModelAnchor("world", 15.0, 64.0, 25.0, 0f, 0f));
    assertNotNull(renderer.lastTeleport.get());
    assertEquals(15.0, renderer.lastTeleport.get().getX());

    // Update part transform
    boolean partUpdated =
        renderer.updatePartTransform(handle, itemPart.partId(), ModelTransform.IDENTITY, 5);
    assertTrue(partUpdated);
    assertFalse(
        renderer.updatePartTransform(
            handle, new ModelPartId("nonexistent_part"), ModelTransform.IDENTITY));

    // Resource count
    int count = renderer.resourceCount(eventId);
    assertTrue(count > 0);
    assertEquals(0, renderer.resourceCount(EventInstanceId.generate()));

    // Reconcile entities
    List<DiscoveredModelEntity> discovered = renderer.reconcileEntities();
    assertNotNull(discovered);
    assertFalse(discovered.isEmpty());

    // Cleanup instance
    renderer.cleanupInstance(eventId);
    assertEquals(0, renderer.resourceCount(eventId));

    // Remove model
    boolean removed = renderer.removeModel(handle);
    assertTrue(removed);

    // Remove all
    renderer.removeAll();
  }
}
