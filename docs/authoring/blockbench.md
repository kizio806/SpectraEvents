# Blockbench Authoring Guide

Status: **experimental exporter; no supported server import or resource-pack pipeline**.

The repository contains
`tools/blockbench/spectraevents-exporter/spectraevents_exporter.js`. Load it from Blockbench's local
plugin picker to experiment with the Spectra bundle format. Generic Model projects with descriptive
group IDs, deliberate pivots and bounded texture sizes are the intended authoring source.

Do not treat exporter output as deployable content. The current beta does not import the bundle into
the server, compile Minecraft model/texture JSON, build a verified ZIP, publish it to Modrinth, or
send it to players. The supported runtime visuals come from native YAML model definitions extracted
to `plugins/SpectraEvents/models/`.

Security and compatibility checks for archive entry count, total expanded bytes, JSON depth, model
complexity, texture dimensions and animation tracks are release requirements for the deferred asset
milestone. Until those checks and an end-to-end pack test exist, asset commands fail closed.
