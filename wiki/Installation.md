# Installation

## Requirements

- Java 25 for current server distributions.
- A supported Minecraft server/version from [[Compatibility]].
- Exactly one SpectraEvents JAR matching the platform family.
- A clean backup before replacing any existing beta installation.

## First boot

1. Stop the server.
2. Put the matching SpectraEvents JAR in `plugins/`.
3. Start the server and wait for the SpectraEvents startup summary.
4. Run `/spectraevents template list` and install one bundle with
   `/spectraevents template install <id>`.
5. Run `/spectraevents doctor` and `/spectraevents definition validate`.
6. Resolve every reported error before starting an event.

## Important beta limitation

The current distribution embeds an installable YAML/model bundle for each official event. This
proves packaging only: the visual assets, complete server journeys, and real-client acceptance are
still release-blocked. Treat the bundles as development material until [[Official-Events]] and
[[Known-Limitations]] say otherwise.

Do not copy missing assets from random releases or enable a required pack with an empty URL/hash.
The supported installer and readiness gate will replace this temporary limitation.
