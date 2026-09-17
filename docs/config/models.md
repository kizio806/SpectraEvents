# Model Specification

SpectraEvents relies on modern Minecraft display entities (`ItemDisplay`, `BlockDisplay`, `TextDisplay`) rather than heavy custom skeletal rendering engines (like ModelEngine) for its core primitives.

## Model Parts
A model can be single-part or multipart. Each part is defined as a distinct display entity.

```yaml
models:
  meteor_core:
    parts:
      main_body:
        type: block
        block: OBSIDIAN
        scale: [3.0, 3.0, 3.0]
        offset: [0.0, 1.5, 0.0]
      fire_aura:
        type: item
        item: MAGMA_CREAM
        scale: [4.0, 4.0, 4.0]
        billboard: center
```

## Supported Properties
- `type`: `block`, `item`, or `text`.
- `offset`: Local position relative to the instance's origin.
- `rotation`: Yaw and pitch.
- `scale`: 3D scale vector.
- `visibility`: Default visibility state.
- `billboard`: Constraints on facing the player (`fixed`, `vertical`, `horizontal`, `center`).

By keeping models simple and leveraging native display interpolation, the engine minimizes server-side packet calculation overhead. External rendering plugins (ModelEngine) can be integrated later as a custom `type`.
