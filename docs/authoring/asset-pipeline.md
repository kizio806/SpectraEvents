# Asset Pipeline Status

The Blockbench importer and resource-pack pipeline are experimental and **unavailable in the current
beta**. `ResourcePackBuilder` fails closed and asset commands must not claim that a ZIP was created.

The active visual workflow is YAML model definitions in `plugins/SpectraEvents/models/`, rendered by
native Display and Interaction entities. The exporter at
`tools/blockbench/spectraevents-exporter/spectraevents_exporter.js` is an authoring experiment, not a
supported end-to-end delivery path.

Before this feature can be enabled, it must have:

1. bounded and hostile-input-tested `.bbmodel`/archive parsing;
2. deterministic item/model/texture output;
3. a real ZIP with validated paths, manifest and hashes;
4. an integration test that consumes the ZIP as a Minecraft resource pack;
5. explicit manual-host and Modrinth delivery policies, including decline/failure behavior.

No official pack is published or delivered automatically by this beta.
