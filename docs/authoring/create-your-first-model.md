# Create a Model Definition

There are two supported authoring paths:

- Native YAML models in `plugins/SpectraEvents/models/`, best for simple Display/Interaction models.
- A Blockbench Generic Model exported to `plugins/SpectraEvents/assets/source/*.spectra.zip`, best for
  textured cube geometry and named animations.

On first start, SpectraEvents extracts working Meteor, Airdrop, Metin, Piñata and Boss Portal YAML examples. Copy one,
change its `id`, parts, transforms and interactions, then restart and validate the load log before
referencing the new ID from an event definition.

For Blockbench, follow the [Blockbench authoring guide](blockbench.md). The import runs at startup on
both distributions; Paper administrators can also use `/spectraevents assets build` or `/spectraevents assets import
<bundle.spectra.zip>`. The imported model ID and animation names are available to the same event
actions as native models:

```yaml
on-enter:
  - type: spawn_model
    model: meteor
  - type: play_animation
    model: meteor
    animation: spin
```

Use `/spectraevents model list` and the Paper model tools exposed by `/spectraevents help` to inspect
registered definitions. For imported assets, use `/spectraevents assets list`, `info`, and `validate` on Paper. Spigot
reports Paper-only command tooling as unsupported rather than silently ignoring it.

The M2 pack workflow has real-client acceptance. See [Asset Pipeline](asset-pipeline.md) for the
profile-specific verification boundary.
