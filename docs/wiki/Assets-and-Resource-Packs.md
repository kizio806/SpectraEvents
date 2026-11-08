# Assets & Resource Pack Pipeline Architecture

SpectraEvents features an automated, deterministic 3D asset compilation and resource pack generation pipeline that compiles Blockbench models into vanilla-compatible Minecraft resources.

---

## 1. Authoring & Compilation Lifecycle

The asset pipeline follows a strictly validated four-stage lifecycle:

1. **Blockbench 3D Authoring:** Models are authored as pure cuboid geometry in Blockbench (`.bbmodel`). Unsupported arbitrary polygon meshes or curved NURBS surfaces are strictly rejected by `BlockbenchModelParser` rather than approximated, guaranteeing identical rendering between editor and vanilla clients.
2. **Spectra Bundle Packaging:** Models and textures are packaged into validated `.spectra.zip` bundles containing geometry definitions, keyframed animation timelines, and texture maps.
3. **Deterministic Pack Generation:** `ResourcePackBuilder` compiles the bundles into deterministic ZIP archives. File order and timestamp normalizations ensure that identical input geometry produces byte-for-byte identical output SHA-1 hashes across build environments.
4. **Runtime Rendering:** Display entities (`ITEM_DISPLAY`, `BLOCK_DISPLAY`) render the assembled model components, while `Interaction` entities provide precision hitboxes mapped to keyframed animation bones.

---

## 2. Target Profiles & Format Specifications

Minecraft resource pack formats evolve across major releases. SpectraEvents compiles distinct target profiles matching each release line:

| Target Profile | Minecraft Release Line | Resource Pack Format | `min_format` / `max_format` |
|---|---|---|---|
| `PROFILE_26_1` | Minecraft 26.1.x | 84.0 | `[84, 0]` |
| `PROFILE_26_2` | Minecraft 26.2.x | 88.0 | `[88, 0]` |
| `PROFILE_26_3` | Minecraft 26.3.x | 97.1 | `[97, 1]` |

---

## 3. Cryptographic Integrity & Protocol Mandates

- **Protocol-Mandated SHA-1:** Minecraft protocol requires resource packs to be identified via a 40-character hexadecimal SHA-1 digest. `ResourcePackBuilder` calculates this digest deterministically during compilation.
- **Fail-Closed Delivery:** When automatic resource pack delivery is enabled, the server adapter verifies status responses (`ACCEPTED`, `DOWNLOADED`, `SUCCESSFULLY_LOADED`). Any client failure or rejection (`DECLINED`, `INVALID_URL`, `FAILED_RELOAD`) triggers fail-closed enforcement according to the server configuration.

---

## 4. Asset Quality & Optimization Standards

- **Texture Dimensions:** Official asset models standardize on optimized power-of-two texture maps ($512 \times 512$ or $1024 \times 1024$). Uncompressed $4096 \times 4096$ textures and transparent placeholder textures are blocked by pipeline validation.
- **Bone Hierarchy:** Models must define a clear root bone hierarchy with normalized pivots $(0, 0, 0)$ to guarantee correct translation interpolation during keyframed timeline playback.
