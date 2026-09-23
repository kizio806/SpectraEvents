# 3D Model Authoring Guide

> [!NOTE]
> New to SpectraEvents? Start with [The Asset Pipeline](asset-pipeline.md), learn about
> [Blockbench Authoring](blockbench.md), or [Create Your First Model](create-your-first-model.md).

SpectraEvents renders 3D models using Minecraft's **Display entity** API (introduced in 1.19.4).
No resource pack or NMS is required for the built-in vanilla-block models. Custom geometric models
are loaded from `.spectra.zip` Blockbench bundles via the Asset Pipeline.

---

## Model YAML Structure

Each file in `plugins/SpectraEvents/models/` defines one model. The top-level keys are:

| Key | Required | Description |
|---|---|---|
| `id` | yes | Unique model identifier. Referenced by `spawn_model` actions. |
| `parts` | yes | Map of named display-entity parts that form the model hierarchy. |
| `interactions` | no | Named invisible interaction hitboxes for click detection. |
| `animations` | no | Named keyframe animations played by `play_animation` actions. |

---

## Parts (`parts`)

Every part maps to one Minecraft Display entity. Parts can reference a `parent:` part ID to form a
hierarchy. Child parts are offset *relative to their parent*, not the world origin.

### Part Types

| `type` | Entity | Required field |
|---|---|---|
| `item_display` | ItemDisplay | `item` — namespaced item ID |
| `block_display` | BlockDisplay | `block` — block state string |
| `text_display` | TextDisplay | `text` — MiniMessage string |

### Item ID formats

| Source | Format | Example |
|---|---|---|
| Vanilla | `minecraft:<item>` | `minecraft:magma_block` |
| Nexo | `nexo:<id>` | `nexo:my_custom_sword` |
| Oraxen | `oraxen:<id>` | `oraxen:my_gem` |
| ItemsAdder | `itemsadder:<namespace>:<id>` | `itemsadder:my_pack:gem` |

### Transform fields (`transform`)

| Field | Type | Default | Description |
|---|---|---|---|
| `translation` | `[x, y, z]` | `[0, 0, 0]` | Position offset from parent or spawn point (blocks) |
| `rotation.euler` | `[pitch, yaw, roll]` | `[0, 0, 0]` | Rotation in degrees around each axis |
| `scale` | `[x, y, z]` | `[1, 1, 1]` | Size multiplier per axis (`2.0` = twice as large) |
| `pivot` | `[x, y, z]` | `[0, 0, 0]` | Rotation/scale origin point |

### Render properties (`render`)

| Field | Values | Default | Description |
|---|---|---|---|
| `billboard` | `fixed`, `center`, `vertical`, `horizontal` | `fixed` | `fixed` = world-aligned; `center` = always faces player |
| `brightness` | `0`–`15` | `15` | Block-light override. `15` = always fully lit |
| `shadow_radius` | Float | `0.0` | Ground shadow size in blocks |
| `shadow_strength` | Float | `1.0` | Shadow opacity (0 = invisible) |
| `view_range` | Float | `64.0` | Visibility distance in blocks |

---

## Interactions (`interactions`)

Interaction entities detect right-click and left-click events without a physical body.
Each interaction entry fires the `interaction` trigger on its owning event instance.

```yaml
interactions:
  main:
    parent: core      # Anchor to a part; the hitbox follows that part's position
    width: 3.0        # Bounding box width in blocks
    height: 3.0       # Bounding box height in blocks
```

---

## Animations (`animations`)

Animations are named keyframe sequences referenced by `play_animation` actions in event YAML.

### Animation header

| Field | Values | Default | Description |
|---|---|---|---|
| `duration` | `Ns`, `Nms`, `Nm` | — | Total animation length (`3s`, `500ms`, `2m`) |
| `loop` | `ONCE`, `LOOP`, `PING_PONG` | — | Playback mode after the last keyframe |
| `recovery` | `RESTART`, `RESUME`, `STOP` | — | Behaviour when the event instance restarts after a server crash |

**Recovery policies:**
- `RESTART` — animation restarts from the beginning (good for entry animations like `fall`)
- `RESUME` — animation resumes from the saved elapsed position (good for idle loops like `pulse`)
- `STOP` — animation stays at its final keyframe (good for one-shot transitions like `collapse`)

### Tracks

Each animation declares a `tracks` map keyed by part ID. A track contains one or more
keyframe channels: `translation`, `rotation`, or `scale`.

```yaml
animations:
  pulse:
    duration: 2s
    loop: LOOP
    recovery: RESUME
    tracks:
      core:
        scale:
          - at: 0s
            value: [2.0, 2.0, 2.0]
            easing: ease_in_out   # Optional; omit for linear interpolation
          - at: 1s
            value: [2.2, 2.2, 2.2]
            easing: ease_in_out
          - at: 2s
            value: [2.0, 2.0, 2.0]
```

### Easing curves

| Value | Description |
|---|---|
| `linear` | Constant interpolation speed (default when omitted) |
| `ease_in` | Starts slow, accelerates — good for falling, gravity |
| `ease_out` | Starts fast, decelerates — good for landing, settling |
| `ease_in_out` | Slow at both ends, fast in the middle — good for breathing loops |

---

## Multi-Part Hierarchy Example

The following excerpt from `meteor.yml` shows a hierarchy with a root part, a child glow part,
and rock shard children with distinct offsets and rotations:

```yaml
id: meteor_core

parts:
  core:
    type: item_display
    item: minecraft:magma_block
    transform:
      scale: [2.2, 2.2, 2.2]
    render:
      billboard: fixed
      brightness: 15

  glow:
    type: item_display
    parent: core            # Attached to core; moves with it
    item: minecraft:shroomlight
    transform:
      scale: [0.85, 0.85, 0.85]  # Fits inside the core block

  shard_n:
    type: item_display
    parent: core
    item: minecraft:blackstone
    transform:
      translation: [0.0, 0.3, -1.4]    # 1.4 blocks north of core
      rotation:
        euler: [-20.0, 0.0, 15.0]       # Angled outward
      scale: [0.7, 0.85, 0.55]
    render:
      brightness: 8                      # Slightly darker than core
```

---

## Built-in Event Models

| Model ID | File | Parts | Animations |
|---|---|---|---|
| `meteor_core` | `models/meteor.yml` | core, glow, 4 shards, 3 trail, label | fall, impact, pulse, collapse |
| `airdrop_crate` | `models/airdrop.yml` | body, lid, 2 straps, parachute, inner, 4 lines, label | descent, land, open |
| `metin_stone` | `models/metin.yml` | base, stone, 4 crystals, 3 runes, crown, label | spawn, pulse, enraged |
| `pinata` | `models/pinata.yml` | hook, rope, body, dome, tail, 4 legs, label | idle, hit, break |
| `boss_portal` | `models/boss-portal.yml` | 4 pillars, 3 arch, gate, 4 eyes, 2 rings, label | opening, active, close |

---

## Administrative Commands

| Command | Description |
|---|---|
| `/spectraevents model list` | Lists all registered model definitions |
| `/spectraevents model info <id>` | Shows part hierarchy and render properties |
| `/spectraevents model validate <id>` | Validates the compiled model definition |
| `/spectraevents model spawn <id>` | Spawns a model preview at your location |
| `/spectraevents model remove <runtime-id>` | Despawns an active model instance |

---

## Tips for Realistic Models

1. **Use emissive blocks for lighting.** `shroomlight`, `magma_block`, `glowstone`, `sea_lantern`,
   and `jack_o_lantern` are always fully lit in vanilla — no brightness override needed.
2. **Layer a brighter block inside a transparent outer shell** (e.g. shroomlight inside magma)
   for a glowing-core look without resource packs.
3. **Stagger animation keyframes** across children to create secondary motion. Children that animate
   50–200 ms *after* the parent feel physically accurate (pendulum lag, jelly bounce).
4. **Use `ease_in` on falling objects** and `ease_out` on landing impacts. This matches real gravity.
5. **Combine `ONCE` and `LOOP` in the same phase** by playing a `ONCE` intro animation on-enter
   and then immediately starting a `LOOP` idle. The ONCE completes; the LOOP takes over.
6. **Keep interaction hitboxes 10–20% larger** than the visual model. Players aim at the visual
   centre, but the hitbox needs to forgive slight misclicks.
