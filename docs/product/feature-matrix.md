# Feature Matrix

This matrix compares several target reference events to identify common mechanical needs. It is an analytical tool to discover shared primitives, not a rigid promise of implementation.

| Feature / Mechanic | Meteor | Airdrop | Metin | Pinata | Vault | Boss Portal |
| :--- | :---: | :---: | :---: | :---: | :---: | :---: |
| **3D Model** | Yes | Yes | Yes | Yes | Yes | Yes |
| **Multipart Model** | Yes | Yes | No | Yes | Yes | Yes |
| **Timer** | Yes | Yes | Yes | Yes | Yes | Yes |
| **Health** | Yes | No | Yes | No | No | No |
| **Hit Counter** | No | No | No | Yes | No | No |
| **Interaction (Click)** | Yes | Yes | No | Yes | Yes | Yes |
| **Leaderboard** | Yes | No | Yes | Yes | No | Yes |
| **Loot / Rewards** | Yes | Yes | Yes | Yes | Yes | Yes |
| **Reward Ranking** | Yes | No | Yes | No | No | Yes |
| **Mob Waves** | No | No | Yes | No | No | Yes |
| **Event Area** | Yes | Yes | Yes | Yes | Yes | Yes |
| **PvP Controls** | Yes | Yes | Yes | Yes | Yes | Yes |
| **Bossbar** | Yes | Yes | Yes | Yes | Yes | Yes |
| **Hologram** | Yes | Yes | Yes | Yes | Yes | Yes |
| **Animations** | Yes | Yes | No | Yes | Yes | Yes |
| **Particles** | Yes | Yes | Yes | Yes | Yes | Yes |
| **Sounds** | Yes | Yes | Yes | Yes | Yes | Yes |
| **Spawn Strategy** | Random | Random | Fixed | Random | Fixed | Fixed |
| **Location Announcement** | Yes | Yes | Yes | Yes | Yes | Yes |
| **Restart Recovery** | Yes | Yes | Yes | Yes | Yes | Yes |

## Common Engine Primitives Identified

From the matrix above, it is clear that almost all events rely on a core set of shared primitives. Rather than building bespoke logic for each event, the engine MUST implement:

1. **Timer**: Used across Meteor, Airdrop, Vault, and Boss Portal for locking phases and expirations.
2. **Health**: Used by Meteor and Metin for tracking damage.
3. **Hit Counter**: Used by Pinata for discrete interactions.
4. **Interaction**: Used across almost all events.
5. **Leaderboard**: Used for tracking participation in damage (Meteor, Metin) and hits (Pinata).
6. **Loot**: A universal requirement.
7. **Event Area**: Spatial presence and rule enforcement.
8. **Mob Waves**: Specifically needed for Metin and Boss Portal.
