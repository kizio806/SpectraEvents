# Spectra Bundle v1

A Spectra Bundle is an installable event template. It is not an active event definition: extracting
the plugin exposes the archive in `plugins/SpectraEvents/templates/`, while only the installer may
write its declared definition to `plugins/SpectraEvents/events/`.

Install the bundled reference template with:

```text
/spectraevents template install metin
```

The installer verifies the archive before any server-owned definition or asset is replaced. It
rejects duplicate ZIP paths, path traversal, unknown manifest fields, malformed event YAML,
unavailable dependencies, incompatible minimum plugin versions, and collisions with existing
active files.

## Archive layout

Bundle v1 deliberately has a small, fixed layout:

```text
metin.spectra.zip
├── manifest.yml
├── event.yml
└── models/
    └── metin_stone.bbmodel
```

`textures/` and `animations/` are reserved for a later external-asset format. In v1, a canonical
Blockbench `.bbmodel` embeds its texture data and named animations, so a second copy would be
ambiguous. Arbitrary metadata and undeclared files are rejected rather than silently ignored.

```yaml
schema-version: 1
id: metin
version: 1.0.0
minimum-spectraevents-version: 0.1.1-beta.2
event-definitions:
  - event.yml
assets:
  - models/metin_stone.bbmodel
dependencies: []
```

All fields are required. Versions use SemVer. Dependency resolution is intentionally not a
marketplace in v1: a non-empty `dependencies` list fails closed until a future local resolver can
verify every dependency deterministically.

After installation, run `/spectraevents validate`; it rebuilds current asset sources and validates
event YAML, duplicate IDs, model references, and animation references before a start.
