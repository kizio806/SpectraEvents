# Blockbench Authoring Guide

This guide explains how to properly structure, animate, and export 3D models from Blockbench for use in SpectraEvents.

## 1. Project Format & Setup

### Recommended Format
**Generic Model** is the recommended format for SpectraEvents.

### Unsupported Formats
Java Block/Item, Bedrock, GeckoLib, Modded Entity formats are not strictly supported, though the exporter may attempt a best-effort export. Stick to **Generic Model**.

### Security Restrictions
SpectraEvents strictly enforces security upon import:
- Arbitrary JavaScript is **not** executed.
- MoLang expressions are **not** executed.
- Texture files are validated against size limits (max 1024x1024, max 1.5MB).
- ZIP path traversal vulnerabilities are strictly guarded.
- Network access is disabled during import.

## 2. Exporter Installation

SpectraEvents comes with a custom Blockbench exporter script.

1. Locate the exporter at `tools/blockbench/spectraevents-exporter/spectra_exporter.js` in the repository.
2. In Blockbench, go to **File** -> **Plugins**.
3. Click the **Load Plugin from File** icon (folder icon).
4. Select `spectra_exporter.js`.
5. Verify it is active by checking if **Export Spectra Bundle** is available under **File** -> **Export**.

## 3. Model Hierarchy & Pivots

A clean hierarchy is critical for runtime targeting (like hitboxes) and animations.

### Best Practices
- **GOOD**: `root` -> `body`, `core`, `outer_ring`, `left_arm`, `right_arm`
- **BAD**: `group1`, `cube17`, `test`, `asdf`

Name your groups descriptively, as these IDs act as your runtime identifiers for animations and interactions.

### Pivots
Pivots dictate the origin point for rotation and scaling.
For example, a door should have its pivot on the hinge, not in the center of the model. SpectraEvents respects the Blockbench pivot completely.

## 4. Textures and UV

You do not need to manually copy PNG files to the resource pack. SpectraEvents extracts embedded textures and writes them to the correct `textures/spectra/...` namespace internally.

### UV Mapping Support
- **Box UV**: Supported.
- **Per-Face UV**: Supported.

## 5. Animation Authoring

SpectraEvents translates Blockbench animations into native server-side interpolations.

### Supported Properties
- **Translation** (Movement)
- **Rotation**
- **Scale**

### Interpolation Support Matrix

The animation compiler translates Blockbench interpolations to server-side math.

| Interpolation | Status | Notes |
|---|---|---|
| Linear | **Supported** | 1:1 direct translation |
| Step | **Supported** | Snaps to keyframe |
| Catmull-Rom | **Approximated** | Falls back to Linear/Bezier depending on internal heuristics |
| Bezier | **Approximated** | Falls back to Linear |
| Smooth | **Approximated** | Falls back to Linear |

*Note: Unsupported inputs will be approximated to the nearest supported curve.*

### Continuous Rotation vs Orientation
SpectraEvents supports **continuous rotations**. If you define a keyframe rotating from `0°` to `720°`, the server will execute two full visual spins rather than interpolating the shortest path (orientation).

## 6. Exporting

1. Complete your model and animations.
2. Go to **File** -> **Export** -> **Export Spectra Bundle**.
3. Save the resulting `.spectra.zip` file.
4. Import it to the server using `/event assets import <file>`.

## Feature Matrix

| Feature | Support Status |
|---|---|
| Cubes | **Supported** |
| Groups/hierarchy | **Supported** |
| Pivot/origin | **Supported** |
| Textures | **Supported** |
| Box UV | **Supported** |
| Per-face UV | **Supported** |
| Translation animation | **Supported** |
| Rotation animation | **Supported** |
| Scale animation | **Supported** |
| Continuous rotations | **Supported** |
| Interactions (Hitboxes) | **Partially supported** (Defined via YAML) |
| Meshes | **Unsupported** |
| MoLang | **Unsupported** |
| Scripts | **Unsupported** |
