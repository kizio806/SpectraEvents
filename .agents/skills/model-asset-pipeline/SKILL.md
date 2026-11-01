---
name: model-asset-pipeline
description: Compile, validate, and verify Blockbench 3D cuboid models, keyframed timeline animations, and resource pack asset delivery.
---

# Model Asset Pipeline Skill

## When to use it
Use this skill when adding, updating, or validating Blockbench `.bbmodel` or `.spectra.zip` bundles and client resource packs.

## Required steps
1. **Model Validation:** Validate that geometry uses supported cuboid elements and valid UVs.
2. **Animation Compilation:** Compile timeline keyframes (`RotationKeyframe`, `ScaleKeyframe`, `TimelineCue`).
3. **Resource Pack Verification:** Ensure resource pack profiles are generated cleanly without corrupted textures or missing descriptors.
4. **Test Execution:**
   ```bash
   ./gradlew :adapters:assets-blockbench:test :adapters:assets-delivery:test
   ```
