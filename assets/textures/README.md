# Official texture sources

These 1024x1024 PNG files are the reviewed raster sources embedded into the five official
Blockbench projects. Run `python3 scripts/assets/sync_official_textures.py` after replacing a PNG;
the script preserves geometry/animations, rescales existing UV coordinates to the 1024 atlas, and
updates only the embedded texture metadata/content.

## Provenance

All five images were generated for SpectraEvents with OpenAI's built-in image generation tool on
2026-10-11, without external reference images, logos, text, or third-party source textures. They are
maintained with the project under MPL-2.0. Human visual review confirmed the intended material and
absence of obvious text/watermarks; Minecraft client acceptance remains a separate release gate.

| File | Intended material | Generation prompt summary |
| --- | --- | --- |
| `airdrop_crate.png` | Charcoal/olive reinforced supply-crate metal with restrained orange wear | Seamless stylized industrial game texture, material-only, opaque, no text/logo |
| `meteor_core.png` | Fractured obsidian/basalt with molten orange-red fissures | Seamless stylized volcanic game texture, material-only, opaque, no scene |
| `metin_stone.png` | Arcane slate and violet/cyan crystal seams | Seamless stylized fantasy stone/crystal texture, no text or recognizable runes |
| `pinata.png` | Layered magenta, cyan, yellow, coral, and violet tissue fringe | Seamless handcrafted paper-fringe texture, no object, candy, text, or logo |
| `boss_portal.png` | Obsidian/purple portal stone with controlled cyan/violet seams | Seamless dark-fantasy carved stone texture, no portal scene or central rune |

The final prompts also required square 1024 output, tileable edges, even material lighting, no
border, and no watermark. Generated 1254px masters were deterministically downsampled to 1024px
with Lanczos filtering before inclusion.
