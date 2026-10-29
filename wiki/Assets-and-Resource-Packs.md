# Assets and Resource Packs

The authoring path is Blockbench cuboid geometry → validated `.spectra.zip` bundle → deterministic
Minecraft resource-pack ZIP → model/animation runtime. Unsupported mesh geometry is rejected rather
than approximated.

Current releases build separate pack profiles for Minecraft 26.1, 26.2, and 26.3. Building a ZIP is
not proof that a real client accepts or renders it correctly. Release evidence must record the client
version, profile, SHA-1, download/load result, reconnect behavior, model visibility, UVs, pivots,
scale, hitbox, animation, and rejection UX.

Player delivery remains disabled by default. A required pack must fail closed when its descriptor,
URL, hash, profile, download, or load result is invalid. Professional official assets normally use
optimized 512/1024 textures; transparent placeholders and unjustified 4096² textures are not
release-quality.
