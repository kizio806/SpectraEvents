# Source Hygiene, Comments, and Suppressions

## Purpose
SpectraEvents upholds the highest standards of code hygiene. Code should be self-documenting and clean, with zero technical debt markers and zero compiler warning suppressions.

## Standard
- **Zero Markers:** No `TODO`, `FIXME`, or `XXX` tags in Java files. All tasks must be resolved or tracked in issues/goals, never left in code comments.
- **Zero Wildcard Imports:** `import java.util.*;` is strictly prohibited. Use explicit single-class imports.
- **Zero `@SuppressWarnings`:** Warning-silencing annotations of any kind are strictly forbidden across all modules. Resolve deprecations, type mismatches, and linter warnings at the source.
- **Comment Discipline:** Meaningful comments explaining non-obvious algorithms, mathematical proofs (e.g. quaternions), or external protocol quirks are permitted. Trivial, redundant comments that restate the code are strictly forbidden.

## Do:
```java
// Correct: Explaining mathematical invariants
// Normalize quaternion to avoid rotational drift after repeated transformations
float norm = (float) Math.sqrt(w * w + x * x + y * y + z * z);
```

## Don't:
```java
// Bad: Redundant narration
// Set current health to current health minus damage
this.currentHealth = this.currentHealth - damage;

// Bad: Leftover markers
// TODO: implement reward claim logic later

// Bad: Warning suppression
@SuppressWarnings("deprecation")
```

## Verification:
```bash
./scripts/verification/validate-source-hygiene.sh
```
