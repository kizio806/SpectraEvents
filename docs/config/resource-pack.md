# Resource-Pack Configuration

SpectraEvents builds a local ZIP in `plugins/SpectraEvents/cache/resource-pack/`. Release CI also
builds three deterministic archives and publishes them to a separate Modrinth **resource-pack**
project: one each for Minecraft 26.1, 26.2, and 26.3. Player delivery remains opt-in and disabled
on a fresh server until real-client acceptance is recorded. On first start, each distribution
creates `plugins/SpectraEvents/resource-pack.yml`:

```yaml
# Recommended: use the resource-pack Modrinth project created for release CI.
enabled: false
required: false
url: ""
sha1: ""
modrinthProjectId: ""
# Empty selects <plugin-version>+<matching Minecraft release line> automatically.
# Use a value only as an explicit reviewed rollback pin.
modrinthVersionId: ""
prompt: "<yellow>Server resources are required for SpectraEvents.</yellow>"
```

## Recommended Modrinth delivery

Create a separate Modrinth project whose project type is **Resource Pack**. Store its ID as either
the repository secret or repository variable `MODRINTH_RESOURCE_PACK_PROJECT_ID`; release CI fails
before publishing if it is absent. The existing `MODRINTH_TOKEN` must be allowed to create and
delete versions, because a failed multi-artifact release removes the versions it created.

For each tagged plugin release, CI builds and publishes these versions to that project:

| Running server | Generated ZIP | Modrinth version selected by default |
| --- | --- | --- |
| 26.1.x | `spectraevents-profile_26_1.zip` | `<plugin-version>+26.1` |
| 26.2.x | `spectraevents-profile_26_2.zip` | `<plugin-version>+26.2` |
| 26.3.x | `spectraevents-profile_26_3.zip` | `<plugin-version>+26.3` |

Set `modrinthProjectId` to the real project ID, leave `modrinthVersionId` blank, set
`enabled: true`, and restart. The plugin queries Modrinth using the exact release line and the
`minecraft` resource-pack loader tag. It refuses a profile mismatch, missing version, non-HTTPS ZIP,
missing SHA-1, or ambiguous primary file. A nonblank `modrinthVersionId` is an explicit rollback
override and must still be tagged for the running release line on Modrinth.

## Manual delivery

To use your own HTTPS host instead:

1. Let startup build the pack from verified Blockbench `.bbmodel` or `.spectra.zip` sources in
   `assets/source/`.
2. Upload that exact ZIP to an administrator-controlled HTTPS URL ending in `.zip`.
3. Compute the archive SHA-1 (40 hexadecimal characters) and put it in `sha1`.
4. Set `enabled: true`; set `required: true` only when your event requires the client assets.
5. Restart the server and confirm the log says that resource-pack delivery is ready.

In manual mode, leave both Modrinth fields empty. The configuration rejects HTTP, a URL with user
information or a fragment, a non-`.zip` path, and an invalid SHA-1. It also rejects mixing a manual
URL/SHA-1 with Modrinth. If resolution fails, no descriptor is placed in the cache and no player is
asked to download a pack. After a successful resolve, every joining player receives the configured
request; the adapter records accepted, declined, failed and loaded statuses. A reconnect creates a
new request from the cached verified descriptor.

The generated archive profile follows the running server: 26.1.x uses `[84, 0]`, 26.2 uses
`[88, 0]`, and 26.3 uses `[97, 1]`. The operator must perform and record real-client acceptance
after changing the target profile, hosted ZIP, or delivery configuration. The server runtime smoke
test proves ZIP generation and server lifecycle only; it does not prove a Minecraft client loaded
the pack.

Release CI reads `config/release/resource-pack-client-evidence.json`. Its committed `disabled`
state is valid while player delivery remains disabled, even though CI publishes the archives to
Modrinth. Before enabling player delivery, replace it with one `loaded` real-client record per
hosted pack/profile, including the exact SHA-1, client version and test date; the release workflow
rejects incomplete or failed evidence.

No Modrinth project ID is hard-coded in the JAR: that would point fresh installations at a project
the administrator does not control. See [ADR 0008](../architecture/adr/0008-automated-modrinth-resource-pack-releases.md).
