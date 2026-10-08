# ADR 0008: Automated Modrinth Resource-Pack Releases

**Status:** Accepted

## Context

ADR 0007 correctly kept player delivery disabled on a fresh installation because no real project
identity or client acceptance evidence existed. It left pack publication and version selection as
manual operator work, which could drift from the plugin release and target resource-pack format.

## Decision

The release workflow builds three deterministic resource-pack ZIPs from the tracked asset sources
and publishes them to a separate Modrinth project of type Resource Pack. Its versions are named
`<plugin-version>+26.1`, `<plugin-version>+26.2`, and `<plugin-version>+26.3`, respectively, and
each is tagged only for the matching Minecraft release line with Modrinth's `minecraft` loader tag.

`resource-pack.yml` remains disabled until the administrator opts in after real-client verification.
When enabled with `modrinthProjectId` and an empty `modrinthVersionId`, the server derives its target
profile from the running Minecraft version and resolves the corresponding release version. A nonempty
version ID is an explicit rollback pin. No project ID is bundled in the plugin.

The workflow creates the GitHub Release only after all four Modrinth versions exist. If a later
publication step fails, it deletes the versions created in that attempt; its token therefore needs
Modrinth version-delete authority.

## Consequences

Plugin releases and resource-pack formats are versioned together without guessing the player's
Minecraft version. A repository owner must configure a real separate Modrinth resource-pack project
and appropriately scoped token before a release can publish. This decision supersedes the manual
publication portion of ADR 0007, while retaining its fresh-install and real-client safety boundary.
