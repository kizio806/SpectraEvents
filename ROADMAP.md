# Project Roadmap — SpectraEvents

This roadmap outlines key milestones, active development tracks, and future vision for **SpectraEvents**.

---

## Current Release Track (v1.0.x — Production Engine Foundation)
- [x] **Hexagonal Architecture:** Decoupled pure Java domain (`spectraevents-core`) and orchestration ports (`spectraevents-application`) free of server APIs.
- [x] **Data-Driven 3D Entity Engine:** Declarative YAML composition with Blockbench display entity translation and hitbox synchronization.
- [x] **Dual Distribution Packaging:** Shaded distribution JARs for Paper/Purpur/Folia and Spigot/CraftBukkit.
- [x] **Transactional State & Recovery:** SQLite persistence with transactional mailbox and restart reconciliation.
- [x] **Automated Modrinth Asset CDN:** Resource pack delivery via Modrinth CDN with SHA-1 validation.

---

## Upcoming Milestones

### Phase 2: Procedural Animations & Cinematic Camera (v1.1.0)
- [ ] **Inverse Kinematics & Procedural Bones:** Dynamic entity animations reacting to terrain elevation and player positioning.
- [ ] **Cinematic Camera Sequences:** Spectator camera paths and smooth spline interpolation for event cutscenes.
- [ ] **Soundscape Spatialization:** Procedural 3D positional audio and ambient music triggers synchronized with event phases.

### Phase 3: Visual Event Studio & Live Editor (v1.2.0)
- [ ] **Web-Based Event Graph Editor:** Node-based visual editor for crafting event phases, conditions, and reward trees.
- [ ] **In-Game Hitbox Gizmo:** Visual particle outline rendering for debugging interaction hitboxes in real time.
- [ ] **Hot-Reload Definition Compiler:** Instant live-reloading of modified YAML definitions without interrupting ongoing instances.

### Phase 4: Multi-Server Synchronization & Network Bus (v1.3.0)
- [ ] **Cross-Server Event Mesh:** Synchronize world boss encounters across multi-proxy BungeeCord and Velocity networks.
- [ ] **Global Leaderboard Sync:** Redis-backed global player participation tracking and tiered reward distribution.

---

## Long-Term Vision
- **Next-Generation Minecraft World Encounters:** The standard 3D interactive world event engine for modern Minecraft networks, combining visual fidelity with enterprise reliability.
