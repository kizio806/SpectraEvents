# Local Model Test Server

Use the dedicated Gradle task to inspect the bundled Blockbench models in a real Minecraft client
without changing the repository's persistent `server/` directory:

```bash
./gradlew --no-daemon runModelTestServer --console=plain
```

The task first imports every bundled `.bbmodel` and builds the 26.1, 26.2 and 26.3 resource-pack
ZIPs. It then starts a fresh Paper 26.2 sandbox in
`distributions/paper/build/local-model-test-server/`. The sandbox contains the current asset sources
and reference event definitions as active test definitions. Its directory is synchronized on every
run, so do not store worlds, configuration, or other durable data there.

## Prerequisite: vanilla-compatible geometry

The current generator produces a vanilla resource pack. Every visible Blockbench element must
therefore be a cube. Meshes, bones with mesh geometry, and arbitrary triangular faces cannot be
represented in vanilla item-model JSON. The task deliberately fails before starting the server if a
source model contains one; convert or recreate that element as cubes in Blockbench and save the
`.bbmodel` again. Reversed cube bounds are normalized, but zero-size cubes are rejected.

## Load the generated pack in the client

Player delivery is intentionally disabled in the test sandbox, because it requires a real HTTPS
host and a recorded client-acceptance result. This manual step tests the model output itself:

1. Run the task successfully once. Its 26.2 ZIP is
   `adapters/assets-blockbench/build/release-resource-packs/spectraevents-profile_26_2.zip`.
2. In the Minecraft launcher profile you will use, open the game directory and copy that ZIP into
   its `resourcepacks/` directory. Do not unzip it.
3. Start Minecraft Java 26.2, enable **SpectraEvents** in Options → Resource Packs, then join
   `localhost:25565`. If the server console does not recognize your account as an operator, run
   `op <your-Minecraft-name>` there and reconnect.
4. In game, run `/spectraevents assets list`, then
   `/spectraevents definition validate`, and start a shipped test definition such as
   `/spectraevents event start metin` or `/spectraevents event start boss_portal`.

This confirms that the client loads the locally generated ZIP and renders the event model. It does
not certify automatic resource-pack delivery; verify that separately with a configured HTTPS or
Modrinth descriptor and record the real-client result before enabling delivery for players.
