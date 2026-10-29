# Modrinth publication source

This directory is the copy-ready source for Modrinth project fields and version entries. It is not
proof of publication. Keep the project **unlisted/private** while the executable P0 release register
is open, then compare every field with Modrinth after an authorized update.

Official Modrinth API field mapping:

| Modrinth field | Ready value/source |
| --- | --- |
| Title | `SpectraEvents` |
| Slug | `spectraevents` if available; otherwise retain the existing project slug |
| Summary / `description` | `Data-driven 3D event engine for Paper, Purpur, Folia, Spigot, and CraftBukkit servers.` |
| Long description / `body` | [`PROJECT_DESCRIPTION.md`](PROJECT_DESCRIPTION.md) |
| Project type | `plugin` |
| Featured categories | `adventure`, `utility` — confirm they remain accepted at submission time |
| Loaders | `paper`, `purpur`, `folia`, `spigot`, `bukkit` |
| Environment | Dedicated server only; client does not install the plugin |
| License ID | `MPL-2.0` |
| Source URL | `https://github.com/kizio806/SpectraEvents` |
| Issues URL | `https://github.com/kizio806/SpectraEvents/issues` |
| Wiki URL | `https://github.com/kizio806/SpectraEvents/wiki` |
| Discord URL | Leave empty until an official support server exists |
| Donation URLs | Leave empty until the owner explicitly adds an official destination |
| Status | `unlisted` or `private` while P0 is open; `approved` only after release authorization |
| Disclosures | Select only disclosures that match the release; do not guess |
| Icon | Supply a reviewed square project icon; none is currently approved in this repository |
| Gallery | Follow [`GALLERY.md`](GALLERY.md); do not upload placeholder or unverified model renders |

Version-specific names, loaders, game versions, files, and changelogs are in
[`VERSION_ENTRIES.md`](VERSION_ENTRIES.md). Release automation already publishes separate platform
and resource-pack versions; never attach incompatible JARs to one entry.
