# Create a Native Model Definition

The supported beta workflow is a YAML model definition under
`plugins/SpectraEvents/models/`. On first start, SpectraEvents extracts working Meteor, Airdrop and
Metin model examples. Copy one, change its `id`, parts, transforms and interactions, then restart the
plugin/server and validate the load log before referencing the new ID from an event definition.

The Blockbench exporter can create experimental source bundles, but server import, resource-pack
building and automatic delivery are disabled. Commands such as `/event assets import` and
`/event assets build` intentionally report that the feature is unavailable.

Use `/event model list` and the Paper model/debug tools that are actually exposed by `/event help` to
inspect loaded definitions. Spigot reports Paper-only tooling as unsupported rather than silently
ignoring it.
