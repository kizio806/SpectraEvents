# Blockbench Authoring Guide

SpectraEvents accepts Blockbench **Generic Model** and **Java Block** projects. Install the local
plugin from `tools/blockbench/spectraevents-exporter/spectraevents_exporter.js`, then select
**File → Export → Export Spectra Bundle**.

Before exporting:

1. Give the project a descriptive name. The exporter converts it to the model ID used in YAML:
   lowercase letters, digits, `_` and `-`, maximum 64 characters.
2. Put visible cubes in groups. Groups become reusable model parts; their Blockbench UUIDs are the
   stable internal part IDs, so do not regenerate them after an event already targets an animation.
3. Use embedded PNG textures only. In Blockbench, embed each texture before export; the exporter
   rejects an external file reference rather than producing an incomplete bundle.
4. Keep cube faces textured and use only standard cube geometry. Mesh elements are rejected by both
   the exporter and the server because vanilla resource-pack item models are cuboid-only. Name animations clearly; supported
   tracks are position, rotation and scale. Blockbench linear, step, and Catmull-Rom curves import
   through the runtime's supported interpolation set.
5. Save the `.bbmodel` directly to `plugins/SpectraEvents/assets/source/`, or export the
   `.spectra.zip` to that directory when a signed portable bundle is wanted.

The exporter writes only `manifest.json`, `model.bbmodel`, and `textures/*.png`, rewrites texture
sources to the bundled PNG paths, and computes the SHA-256 checksums before the archive is saved.
The server independently validates all of that data; exporter output is not trusted merely because it
comes from the supplied plugin.

Restart the server or run `/spectraevents assets build` on Paper. A successful import registers the
canonical `.bbmodel` or bundle model ID for `spawn_model` and its animation names for
`play_animation`. Inspect the import with
`/spectraevents assets list`, `/spectraevents assets info <model-id>`, and `/spectraevents assets validate <model-id>`.

A new target profile or a changed custom-model-data mapping requires real-client acceptance before
it is release-ready.
