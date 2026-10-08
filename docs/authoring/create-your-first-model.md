# Create Your First Model

Create the visual model in Blockbench and save it as a `.bbmodel`; this is the only supported
canonical source. Use either the Generic Model or Java Block format, group visible cubes, add named
animations, and embed PNG/JPEG textures.

Place `my_model.bbmodel` in `plugins/SpectraEvents/assets/source/`, then restart the server. The
filename becomes the model ID (`my_model`) and the animation names in Blockbench are used directly
by event YAML:

```yaml
on-enter:
  - type: spawn_model
    model: my_model
  - type: play_animation
    model: my_model
    animation: idle
```

Alternatively use **File → Export → Export Spectra Bundle** from
`tools/blockbench/spectraevents-exporter/spectraevents_exporter.js` and place the generated
`.spectra.zip` in the same directory. Paper exposes asset inspection and rebuild commands; Spigot
imports the same files at startup but does not expose the Paper-only asset command tools.

Never duplicate geometry or animation names in `models/*.yml`; that directory is not loaded.
