# SpectraEvents Testing Strategy

## Test layers

- Unit tests verify isolated domain and application behavior.
- Architecture tests enforce dependency direction and platform isolation.
- Integration tests use the `integrationTest` source set and task for real infrastructure boundaries.
- Runtime workflows run the built JAR on real Paper, Purpur, Folia, Spigot, and CraftBukkit servers.
- Regression tests reproduce fixed defects before verifying their correction.

Every meaningful domain behavior should have unit tests. Every fixed, reproducible bug should get a
regression test where technically reasonable. Architecture boundaries must be executable rules, not
documentation only. Do not use mocks when behavior materially depends on real Paper behavior, and do
not create meaningless tests to increase coverage.

Mocks do not establish server compatibility. Platform-specific behavior is verified by the
command-driven runtime workflow, including phase transition, cleanup, restart, and recovery.

JUnit 5 runs unit and integration source sets. JaCoCo combines both suites into XML and HTML reports
per Java module. Coverage thresholds will be introduced per module after meaningful logic exists; no
global percentage is imposed on placeholder-free bootstrap code.

Every Java source set is compiled with `-Xlint:all -Werror` and Error Prone, formatted by Spotless
with google-java-format, and checked by Checkstyle, PMD, and SpotBugs. CodeQL performs an explicit
multi-module source-set build in CI before analysis.

The required full quality gate is:

```bash
./gradlew clean check build
```
