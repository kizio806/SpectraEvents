# Quality Gates, Testing, and Verification

## Purpose
The repository enforces a strict zero-tolerance quality pipeline (`check` and `build`). Every pull request and release is validated on CI against compiler warnings, code coverage thresholds, static analysis (SpotBugs, Checkstyle, PMD), and platform contracts.

## Standard
- **Strict Compiler Settings:** All Java subprojects compile with `-Xlint:all` and `-Werror`. No compiler warnings (unchecked conversions, deprecations, raw types, or missing serial IDs) may be left unresolved.
- **Coverage Minimum:** Maintain at least 85% instruction coverage on `spectraevents-core` and `spectraevents-application` (enforced by `jacocoTestCoverageVerification`) and ensure at least 80% patch coverage on new code for Codecov.
- **Domain & Boundary Testing:** Every meaningful business rule, 3D calculation, phase transition, and reward aggregate requires unit tests.
- **Static Analysis & Security Scans:** Zero SpotBugs, Checkstyle, and PMD violations. Global blanket exclusions in `spotbugs-exclude.xml` are prohibited. Achieve 0 open CodeQL security alerts and a Grade A SonarCloud Reliability, Security, and Maintainability rating.
- **Mandatory 100% Green CI Pre-Commit / Pre-Merge Requirement:** An engineer or AI agent must verify that all local checks pass (`./gradlew clean check build`) and monitor PR checks (`gh pr checks <PR> --watch`) until 100% of workflows are GREEN before merging.

## Do:
```java
// Correct: Isolated unit tests covering edge cases and calculations
@Test
void slerpShouldInterpolateBetweenOrientations() {
  Quaternion q1 = Quaternion.IDENTITY;
  Quaternion q2 = Quaternion.fromAxisAngle(Vector3.UP, (float) Math.PI);
  Quaternion mid = q1.slerp(q2, 0.5f);
  assertEquals(0.0f, mid.x(), 1e-5f);
  assertEquals(0.7071068f, mid.y(), 1e-5f);
}
```

## Don't:
```java
// Bad: Suppressing warnings or bypassing tests
@SuppressWarnings("all")
// Bad: Disabling coverage verification tasks in build scripts
```

## Verification:
```bash
./gradlew test
./gradlew jacocoTestCoverageVerification
./gradlew check
```
