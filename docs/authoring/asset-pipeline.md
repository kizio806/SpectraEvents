# Asset Pipeline

SpectraEvents supports signed `.spectra.zip` input exported from a Blockbench **Generic Model** or
**Java Block** project by `tools/blockbench/spectraevents-exporter/spectraevents_exporter.js`.

Place only these files directly in the plugin source directory:

```text
plugins/SpectraEvents/
├── assets/source/
│   └── meteor.spectra.zip
└── generated/resource-pack/
    └── spectraevents-profile_26_1.zip
```

The server imports all source bundles during startup. On Paper, an administrator can also run
`/spectraevents assets build` to scan, validate, register and build again, or `/spectraevents assets import
<file.spectra.zip>` to import one top-level source file and rebuild the ZIP immediately. The source
bundle and the previous generated ZIP are never deleted by `/spectraevents assets clean`; that command only
clears the in-memory import cache.

## Supported bundle contract

The exporter creates exactly this archive layout:

```text
manifest.json
model.bbmodel
textures/texture_0.png
textures/texture_1.png
```

`manifest.json` declares schema version `1`, the safe model ID, `model.bbmodel`, every texture path,
and a SHA-256 checksum for every file other than the manifest. The importer rejects another layout,
an unsupported Blockbench format, a broken checksum, duplicate entries, ZIP-slip paths, external
texture paths, or an invalid model ID. A rejected bundle does not become an event model or alter the
generated pack.

The importer preserves cubes, texture UVs, pivots, group hierarchy and named position/rotation/scale
animations. Every top-level and nested Blockbench group becomes a normal model part. The bundle model
ID is therefore usable in event YAML, and the animation names are usable with `play_animation`.

```yaml
on-enter:
  - type: spawn_model
    model: meteor
  - type: play_animation
    model: meteor
    animation: spin
    loop: true
```

## Safety limits

The current schema intentionally has small, predictable limits: a 5 MB compressed archive; at most
18 ZIP entries, 8 MB expanded data and 2 MB per entry; 16 textures; 512 cubes; 128 groups with
32 levels of nesting; 32 animations; 2,000 keyframes; JSON depth 64; and 1024×1024 / 1,048,576-pixel
textures. Textures must be embedded PNG or JPEG data when exporting. JPEG inputs are transcoded to
PNG before the resource pack is generated. These limits are part of the input contract, not
recommendations.

## Generated resource pack

The builder creates a deterministic ZIP with `pack.mcmeta`, generated model and texture JSON, a
modern `minecraft:custom_model_data` range-dispatch mapping for paper items, and a
`spectraevents-manifest.json` containing generated-file SHA-256 values. The builder also returns
archive SHA-1 and SHA-256 values; SHA-1 is the value Minecraft needs for player delivery.

The pack is generated, structurally tested, and M2 was confirmed by the operator in a real-client
workflow. Record new target-profile validation whenever a Minecraft resource-pack format changes;
client acceptance for one profile does not automatically prove a future profile.

## Delivery boundary

SpectraEvents never publishes a pack or exposes the local generated ZIP automatically. If an operator
chooses to deliver one, they must host that exact ZIP at an administrator-controlled HTTPS `.zip` URL
and configure `plugins/SpectraEvents/resource-pack.yml`. See
[resource-pack configuration](../config/resource-pack.md). Modrinth publishing remains disabled.
