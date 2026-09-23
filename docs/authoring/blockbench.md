# Blockbench Authoring Guide

SpectraEvents accepts **only Blockbench Generic Model** projects. Install the local plugin from
`tools/blockbench/spectraevents-exporter/spectraevents_exporter.js`, open the model in Generic Model
format, then select **File → Export → Export Spectra Bundle**.

Before exporting:

1. Give the project a descriptive name. The exporter converts it to the model ID used in YAML:
   lowercase letters, digits, `_` and `-`, maximum 64 characters.
2. Put visible cubes in groups. Groups become reusable model parts; their Blockbench UUIDs are the
   stable internal part IDs, so do not regenerate them after an event already targets an animation.
3. Use embedded PNG textures only. In Blockbench, embed each texture before export; the exporter
   rejects an external file reference rather than producing an incomplete bundle.
4. Keep cube faces textured and use only standard cube geometry. Name animations clearly; supported
   tracks are position, rotation and scale, with linear or step interpolation.
5. Export the `.spectra.zip` directly to `plugins/SpectraEvents/assets/source/`.

The exporter writes only `manifest.json`, `model.bbmodel`, and `textures/*.png`, rewrites texture
sources to the bundled PNG paths, and computes the SHA-256 checksums before the archive is saved.
The server independently validates all of that data; exporter output is not trusted merely because it
comes from the supplied plugin.

Restart the server or run `/spectraevents assets build` on Paper. A successful import registers the bundle
model ID for `spawn_model` and its animation names for `play_animation`. Inspect the import with
`/spectraevents assets list`, `/spectraevents assets info <model-id>`, and `/spectraevents assets validate <model-id>`.

M2 real-client rendering was confirmed by the operator. A new target profile or a changed
custom-model-data mapping still requires its own real-client acceptance before it is release-ready.
