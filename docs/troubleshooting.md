# Troubleshooting & FAQ

---

## 🛠️ Common Administrator Issues and Solutions

### 1. 3D models do not render or show missing texture checkerboards
- **Cause:** Client has not accepted the server resource pack, or the resource pack download URL is misconfigured.
- **Solution:** Verify that the player has accepted server resource packs in their server list entry. Run `/spectraevents doctor` to inspect resource pack availability and checksum configuration.

---

### 2. Folia 26.3 startup rejection ("Folia 26.3 is not supported")
- **Cause:** Per ADR 0009, Folia 26.3 is intentionally refused at bootstrap due to unverified region thread boundaries in upstream Folia builds.
- **Solution:** Run on Paper 26.2/26.3 or Folia 26.1–26.2 until Folia 26.3 compatibility is formally certified.

---

### 3. Scoreboard not displaying on Folia
- **Cause:** Folia intentionally leaves Bukkit scoreboard creation and sidebar objectives unsupported across regions.
- **Solution:** SpectraEvents automatically falls back to BossBar HUD notifications when running on Folia servers.

---

### 4. Player rewards not claimed after server crash
- **Cause:** Server was terminated during claim delivery.
- **Solution:** Run `/spectraevents rewards reconcile` in console. The transactional mailbox will resume `PENDING` claims and safely complete delivery to online players.

---

### 5. Event YAML definition fails validation
- **Cause:** Invalid YAML syntax or missing required phase/transition properties.
- **Solution:** Run `/spectraevents definition validate` to print exact source-aware error diagnostics and line numbers.
