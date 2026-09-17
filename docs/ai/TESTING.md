# SpectraEvents Testing Strategy

## Test layers

- Unit tests verify isolated domain and application behavior.
- Architecture tests enforce dependency direction and platform isolation.
- Integration tests use the `integrationTest` source set and task for real infrastructure boundaries.
- Paper smoke tests run the built JAR on a controlled Paper server through `runServer`.
- Regression tests reproduce fixed defects before verifying their correction.

Every meaningful domain behavior should have unit tests. Every fixed, reproducible bug should get a
regression test where technically reasonable. Architecture boundaries must be executable rules, not
documentation only. Do not use mocks when behavior materially depends on real Paper behavior, and do
not create meaningless tests to increase coverage.

MockBukkit is not part of the bootstrap because Paper 26.2 compatibility has not been established.
Paper-specific behavior should be tested on a controlled real server when it is introduced.

JaCoCo produces XML and HTML reports per Java module. Coverage thresholds will be introduced per
module after meaningful logic exists; no global percentage is imposed on placeholder-free bootstrap
code.

The required full quality gate is:

```bash
./gradlew clean check build
```
