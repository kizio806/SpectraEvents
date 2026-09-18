# 3D Model Authoring Guide

SpectraEvents supports data-driven 3D model definitions stored as YAML files in the `plugins/SpectraEvents/models/` directory.

## Model YAML Structure

Each model file defines a single 3D model with a unique `id`, a map of `parts`, and optional `interactions`.

```yaml
id: meteor

parts:
  core:
    type: item_display
    item: minecraft:magma_block
    transform:
      translation: [0.0, 0.0, 0.0]
      rotation:
        euler: [0.0, 0.0, 0.0]
      scale: [2.0, 2.0, 2.0]
      pivot: [0.0, 0.0, 0.0]
    render:
      billboard: fixed
      brightness: 15
      shadow_radius: 1.5
      shadow_strength: 0.8
      view_range: 64.0

  tail:
    type: block_display
    parent: core
    block: minecraft:blackstone
    transform:
      translation: [0.0, 1.0, 0.0]
      scale: [0.8, 1.5, 0.8]

  label:
    type: text_display
    parent: core
    text: "<red><bold>FALLING METEOR</bold></red>"
    alignment: center
    transform:
      translation: [0.0, 2.5, 0.0]

interactions:
  main_hitbox:
    parent: core
    offset: [0.0, 0.0, 0.0]
    width: 3.0
    height: 3.0
```

---

## Field Specifications

### 1. Root Fields
- `id` (String, required): Unique identifier for the model (e.g. `meteor`, `airdrop`, `metin`).

### 2. Part Definitions (`parts.<part_id>`)
- `type` (String, required): One of `item_display`, `block_display`, `text_display`.
- `parent` (String, optional): Part ID of the parent node. If omitted, part is attached to model root.
- `item` (String, required for `item_display`): Item reference (`minecraft:magma_block`, `nexo:meteor_core`, `oraxen:meteor_core`, `itemsadder:namespace:meteor_core`).
- `block` (String, required for `block_display`): Block material or block data string (e.g. `minecraft:obsidian`, `minecraft:oak_log[axis=y]`).
- `text` (String, required for `text_display`): MiniMessage formatted text string.
- `alignment` (String, optional for `text_display`): `center`, `left`, `right`. Default: `center`.

### 3. Transform Fields (`transform`)
- `translation` (Vector3 `[x, y, z]`, default `[0.0, 0.0, 0.0]`): Offset relative to parent or root.
- `rotation.euler` (Vector3 `[pitch, yaw, roll]`, default `[0.0, 0.0, 0.0]`): Rotation in degrees.
- `scale` (Vector3 `[x, y, z]`, default `[1.0, 1.0, 1.0]`): Scaling vector.
- `pivot` (Vector3 `[x, y, z]`, default `[0.0, 0.0, 0.0]`): Origin point for rotation and scaling.

### 4. Render Properties (`render`)
- `billboard`: `fixed`, `center`, `vertical`, `horizontal`. Default: `fixed`.
- `brightness` (Integer 0-15, default `15`): Light level override.
- `shadow_radius` (Float, default `0.0`): Radius of ground entity shadow.
- `shadow_strength` (Float, default `1.0`): Strength/opacity of ground shadow.
- `view_range` (Float, default `64.0`): Entity view range multiplier.

### 5. Interaction Hitboxes (`interactions.<interaction_id>`)
- `parent` (String, optional): Parent part ID anchor.
- `offset` (Vector3 `[x, y, z]`, default `[0.0, 0.0, 0.0]`): Offset from parent anchor.
- `width` (Float, required): Width of interaction bounding box in blocks.
- `height` (Float, required): Height of interaction bounding box in blocks.

---

## Administrative Commands

- `/event model list`: Lists all compiled and registered 3D model definitions.
- `/event model info <id>`: Displays detailed hierarchy and part properties for a model.
- `/event model validate <id>`: Validates a compiled model definition.
- `/event model spawn <id>`: Spawns a 3D model instance preview at player location.
- `/event model remove <runtime-id>`: Despawns an active 3D model instance.
