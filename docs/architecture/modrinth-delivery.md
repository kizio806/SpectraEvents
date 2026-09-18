# Modrinth Resource Pack Delivery Architecture

SpectraEvents employs a robust, cache-first architecture to deliver the official 3D models, item models, and textures to players using Modrinth as its CDN.

## Version Resolution Contract

SpectraEvents strictly resolves exact Modrinth versions. It does not use "latest" fallbacks to guarantee client-server consistency.

The canonical asset profile string is:
`<PLUGIN_VERSION>+<ASSET_PROFILE>`

### Asset Profiles (Minecraft Version Bands)
- `26.1.1` -> `26.1`
- `26.1.2` -> `26.1`
- `26.2` -> `26.2`
- `26.3` -> `26.3`

For example, SpectraEvents version `0.1.0-beta.2` running on Minecraft `26.2` will request exactly the asset release version `0.1.0-beta.2+26.2` from Modrinth.

## Resolution Pipeline

1. **Resolution**: The server queries the Modrinth API for the exact version string.
2. **Extraction**: The primary `.zip` file is located from the release files.
3. **Descriptor Creation**: The server extracts the `HTTPS URL`, `SHA-1`, `SHA-512`, and `size` to create an immutable `ResourcePackDescriptor`.

## Caching Strategy

Modrinth API rate limits are respected by aggressive caching. **There is NO Modrinth API request per player join.**

- **Startup Resolution**: The descriptor is resolved and cached when the server starts.
- **Reload Resolution**: The descriptor is re-resolved only if `/event reload` is run.
- **Cached Player Delivery**: When a player joins, the cached descriptor is immediately used.

## Player State Machine

SpectraEvents tracks the exact resource pack status for every player to orchestrate gameplay safely.

### PlayerResourcePackState
- `PENDING`: The prompt is on the player's screen.
- `ACCEPTED`: The player clicked Yes, download is beginning.
- `DOWNLOADED`: The client finished downloading.
- `DECLINED`: The player clicked No.
- `FAILED_DOWNLOAD`: The client failed to download the zip.
- `SUCCESS`: The pack is fully loaded and applied.

### PlayerAssetReadiness
Asset readiness determines if a player is allowed to see and interact with 3D models.
- `READY`: `PlayerResourcePackState` reached `SUCCESS`.
- `NOT_READY`: The player `DECLINED` or had a `FAILED_DOWNLOAD`.

## Failure Behaviors

- **Modrinth Unavailable**: If the Modrinth API is down during server startup, the plugin will fail to resolve the official pack. Custom packs bypass this.
- **Pack Declined**: If a player declines the pack, their `PlayerAssetReadiness` becomes `NOT_READY`. Models may appear as generic items or be hidden depending on strictness settings.
- **Pack Mismatch**: If `AssetBuildId` mismatches between the server and the client pack (if enforced), the player is marked `NOT_READY` until the pack updates.
