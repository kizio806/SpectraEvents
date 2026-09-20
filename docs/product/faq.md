# SpectraEvents FAQ

## Player FAQ

**Q: Does this beta deliver a SpectraEvents resource pack automatically?**
A: **No.** Resource-pack compilation and delivery are disabled until the project can produce and verify a real ZIP end to end.

**Q: Do I need a client mod?**
A: The current built-in visuals use native server entities and require no client mod. Custom-texture resource-pack support is not available in this beta.

**Q: Why does SpectraEvents not show a resource-pack prompt?**
A: The beta deliberately does not request a pack; the incomplete delivery pipeline is fail-closed.

---

## Server Owner FAQ

**Q: Which plugin file do I install?**
A: Install the Paper-family JAR on Paper/Purpur/Folia or the Spigot-family JAR on Spigot/CraftBukkit. Do not mix the artifacts.

**Q: How do I use my own models?**
A: Author native model YAML manually. The Blockbench-to-pack workflow is experimental and unavailable in the beta.

---

## Maintainer / Asset Author FAQ

**Q: Where do I edit models?**
A: The Blockbench exporter is experimental. `/event assets import` and pack building are unavailable in the beta.

**Q: Where are generated files?**
A: No resource-pack files are generated in this beta.

**Q: Should I edit generated JSON manually?**
A: There is no generated output ZIP to edit.

**Q: How are resource-pack versions named?**
A: No pack version contract is published while generation is disabled.

**Q: Where are official packs published?**
A: No official pack is published by this beta.

**Q: What happens when Minecraft adds a new resource-pack format?**
A: Format mapping will be implemented and tested as part of the deferred asset milestone.
