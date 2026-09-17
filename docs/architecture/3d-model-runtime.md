# Professional 3D Model Runtime Architecture

## Overview

SpectraEvents provides a production-grade, data-driven 3D model engine supporting complex multi-part visual geometry and interaction hitboxes built on top of native Minecraft `Display` and `Interaction` entities.

The architecture strictly enforces separation of concerns across module boundaries:

```text
YAML Authoring (`plugins/SpectraEvents/models/*.yml`)
  ↓
ModelSpec / DTOs (`spectraevents-application`)
  ↓
ModelCompiler (`spectraevents-application`)
  ├── Hierarchy validation & Cycle Detection
  ├── Euler to Quaternion conversion
  └── Precomputed composed transforms
  ↓
ModelDefinition [Immutable] (`spectraevents-core`)
  ↓
ModelDefinitionRegistry (`spectraevents-application`)
  ↓
ModelRuntimeService (`spectraevents-application`)
  ↓
ModelRendererPort [Port] (`spectraevents-application`)
  ├── PaperModelRenderer (`platforms/paper/common`)
  └── SpigotModelRenderer (`platforms/spigot/common`)
```

---

## Architectural Invariants & Boundary Rules

1. **Platform Independence**: Neither `spectraevents-core` nor `spectraevents-application` may import `org.bukkit.*`, `io.papermc.*`, `net.minecraft.*`, `CraftBukkit`, or `JOML`.
2. **Platform-Neutral Primitives**: All math primitives (`Vector3`, `Quaternion`, `EulerRotation`, `ModelTransform`) reside in `spectraevents-core` as zero-dependency immutable value objects.
3. **Atomic Operations**: Spawning a model is atomic. If any part or interaction hitbox fails to spawn, all previously spawned entities in that batch are immediately removed via a rollback mechanism.
4. **Zero Global Tick Costs**: Static 3D models do not require a tick loop or scheduler tasks per entity. Transform calculations are precomputed at compilation or computed on demand.

---

## Domain Model Primitives

- `ModelDefinition`: Compiled immutable representation of a 3D model, containing a list of `ModelPartDefinition` objects and `InteractionDefinition` objects.
- `ModelPartDefinition`: Defines visual part geometry (`ITEM_DISPLAY`, `BLOCK_DISPLAY`, `TEXT_DISPLAY`), local transform, parent part reference, composed transform, asset references, and render properties.
- `InteractionDefinition`: Defines an independent bounding volume (`Interaction` entity) for click/hit detection.
- `ModelAnchor`: Platform-neutral world location reference (`worldName`, `x`, `y`, `z`, `yaw`, `pitch`).
- `ModelRuntimeId`: Unique runtime instance identifier allocated per spawned model instance.
- `RenderedModelHandle`: Opaque handle returned to application code representing active native entity resources.

---

## Transform & Quaternion Mathematics

### 1. Authoring vs Canonical Representation
- **Authoring**: Yaml specs specify rotation using degrees in intrinsic Z-X-Y Euler angles (`[pitch, yaw, roll]`).
- **Canonical**: `ModelCompiler` converts Euler angles into a normalized `Quaternion` $(x, y, z, w)$. All runtime hierarchy compositions operate on quaternions.

### 2. Pivot Math
Pivot points define the center of rotation and scaling:
$$V_{\text{eff}} = \vec{t} + \vec{p} - R \cdot (\vec{s} \circ \vec{p})$$
where $\vec{t}$ is translation, $\vec{p}$ is pivot vector, $R$ is rotation matrix/quaternion, and $\vec{s}$ is scale vector.

### 3. Parent-Child Composition
For a child part with local transform $T_{\text{local}} = (t_1, r_1, s_1, p_1)$ and parent composed transform $T_{\text{parent}} = (t_0, r_0, s_0, p_0)$:
- Composed Scale: $s_{\text{comp}} = s_0 \circ s_1$
- Composed Rotation: $r_{\text{comp}} = r_0 \cdot r_1$
- Composed Translation: $t_{\text{comp}} = t_0 + r_0 \cdot (s_0 \circ t_1)$

---

## Entity Reconciliation & PDC Keys

Native entities store persistent data tags using NamespacedKeys:
- `spectraevents:model_instance_id`: UUID string of the active runtime model.
- `spectraevents:model_definition_id`: String ID of the model definition.
- `spectraevents:model_part_id`: String ID of the part or interaction volume.
- `spectraevents:resource_role`: `VISUAL_PART` or `INTERACTION_HITBOX`.
- `spectraevents:instance_id`: Optional owner `EventInstanceId`.

During server startup, `EntityReconciliationService` scans loaded entities and reconnects valid models or despawns orphaned/corrupted model parts.
