# SpectraEvents FAQ

## Player FAQ

**Q: Does this beta deliver a SpectraEvents resource pack automatically?**
A: **No.** SpectraEvents builds a local ZIP but never hosts or publishes it. An operator may opt in to
delivery after hosting the exact ZIP at an HTTPS URL and configuring its SHA-1.

**Q: Do I need a client mod?**
A: No. Native visuals require no client mod. Textured Blockbench assets use the ordinary Minecraft
resource-pack prompt when an operator enables delivery.

**Q: Why does SpectraEvents not show a resource-pack prompt?**
A: Delivery is disabled by default. It also remains fail-closed when the configured HTTPS URL or SHA-1
is invalid or cannot be resolved.

---

## Server Owner FAQ

**Q: Which plugin file do I install?**
A: Install the Paper-family JAR on Paper/Purpur or Folia 26.1–26.2, or the Spigot-family JAR on
Spigot/CraftBukkit. Folia 26.3 is refused before initialization; install a newer SpectraEvents
version when its Folia support returns. Do not mix the artifacts.

**Q: How do I use my own models?**
A: Save a Blockbench `.bbmodel`, or export a Blockbench Generic Model/Java Block `.spectra.zip`, to
`plugins/SpectraEvents/assets/source/`. See the Blockbench guide before enabling player delivery.

---

## Maintainer / Asset Author FAQ

**Q: Where do I edit models?**
A: Edit the canonical Blockbench `.bbmodel` or use the supplied exporter. Paper provides
`/spectraevents assets import` and `/spectraevents assets build`; both validate the source and update the local ZIP.

**Q: Where are generated files?**
A: `plugins/SpectraEvents/cache/resource-pack/` contains the generated ZIP.

**Q: Should I edit generated JSON manually?**
A: No. Treat the ZIP as generated output; fix the Blockbench source bundle and rebuild it.

**Q: How are resource-pack versions named?**
A: The local filename contains the target profile, for example `spectraevents-profile_26_1.zip`.

**Q: Where are official packs published?**
A: No official pack is published by this beta.

**Q: What happens when Minecraft adds a new resource-pack format?**
A: A new `AssetTargetProfile` and real-client verification are required before that version is
supported for generated assets.
