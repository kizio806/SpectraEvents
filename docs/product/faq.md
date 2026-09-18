# SpectraEvents FAQ

## Player FAQ

**Q: Do I need to install SpectraEvents Assets manually?**
A: **No.** The server handles everything automatically. You do not need to download zip files or place anything in your `resourcepacks/` folder.

**Q: Do I need a client mod?**
A: **No.** SpectraEvents runs 100% on the server and utilizes native Minecraft vanilla resource packs. No client mods (like OptiFine, Fabric, or Forge) are required to see the 3D models or animations.

**Q: Why does the server ask me to accept a resource pack?**
A: SpectraEvents generates real 3D entities and custom UI screens. These visuals require custom item models and textures which are distributed through a resource pack.

**Q: Where is the pack downloaded from?**
A: Official baseline assets are delivered straight from the highly secure and fast **Modrinth CDN**. If a server uses custom assets, they are downloaded from the server's chosen web host.

**Q: What happens if I decline?**
A: If the server marks the pack as required, you may be disconnected. Otherwise, you will remain on the server, but the custom 3D models will likely appear as strange vanilla items or missing textures.

---

## Server Owner FAQ

**Q: Do I need to download SpectraEvents Assets?**
A: **No.** Server owners only need to download the SpectraEvents plugin `.jar`. The resource pack logic is entirely handled internally.

**Q: Do I need to host the official resource pack on a web server?**
A: **No.** SpectraEvents connects to the official Modrinth project (ID: `Rg1nw8IW`, slug: `spectraevents-assets`) and serves the download URLs from there directly to the joining players.

**Q: Do I need a Modrinth API token?**
A: **No.** Retrieving public versions from Modrinth does not require authentication.

**Q: Do players install anything manually?**
A: **No.** They will get the standard Minecraft "Do you want to download this resource pack?" prompt.

**Q: How do I use my own models?**
A: If you want to use custom models (e.g. `my_boss.bbmodel`), you must host the generated resource pack yourself (e.g., S3, Apache, Cloudflare) and configure the `resource-pack` section in `config.yml` to use a `manual` source. See [Resource Pack Config](../config/resource-pack.md).

---

## Maintainer / Asset Author FAQ

**Q: Where do I edit models?**
A: Models are authored in **Blockbench** (Generic Model format). The `.spectra.zip` exports are imported directly via `/event assets import`.

**Q: Where are generated files?**
A: Generated asset data is placed in the plugin's internal cache directory during the `/event assets build` step.

**Q: Should I edit generated JSON manually?**
A: **Never.** Any manual changes to `pack.mcmeta`, `models/`, or `textures/` in the output zip will be completely overwritten during the next pipeline build. Always modify the source in Blockbench and re-export.

**Q: How are resource-pack versions named?**
A: Resource packs are bound to the Plugin Version + Minecraft Version Band (e.g., `0.1.0-beta.2+26.1`). SpectraEvents strictly matches this version contract.

**Q: Where are official packs published?**
A: They are published to the official **SpectraEvents Assets** project on Modrinth (Project ID: `Rg1nw8IW`).

**Q: What happens when Minecraft adds a new resource-pack format?**
A: A new asset profile is added (e.g., `26.2` mapped to `min_format: [88, 0], max_format: [88, 0]`), and the CI builds a specific version of the ZIP tailored for that Minecraft version.
