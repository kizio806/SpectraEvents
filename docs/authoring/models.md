# Model Authoring

Official SpectraEvents visual models use Blockbench `.bbmodel` files as their sole canonical source.
Edit `plugins/SpectraEvents/assets/source/<model-id>.bbmodel` or import a signed
`<model-id>.spectra.zip` exported by the supplied Blockbench plugin. Do not create or edit a
`plugins/SpectraEvents/models/*.yml` file: that legacy path is not part of the runtime.

At startup, SpectraEvents validates and imports every supported source file, then registers its
model ID and named animations for `spawn_model` and `play_animation` in event YAML. The built-in
sources are `meteor_core`, `airdrop_crate`, `metin_stone`, `pinata`, and `boss_portal`.

See [Blockbench authoring](blockbench.md) for the source format and
[Asset Pipeline](asset-pipeline.md) for packaging and validation.
