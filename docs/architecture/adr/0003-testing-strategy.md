# ADR 0003: Testing strategy

## Status

Accepted

## Context

The project needs useful tests from its first commit without creating fake behavior or relying on
incomplete Paper mocks.

## Decision

Use JUnit Jupiter for unit and regression tests, ArchUnit plus Gradle boundary verification for module
isolation, an `integrationTest` source set for infrastructure, and a real Paper server for smoke tests.
Generate JaCoCo XML and HTML reports, but defer coverage thresholds until modules contain meaningful
logic. Do not adopt MockBukkit until target-version compatibility and value are demonstrated.

## Consequences

Architecture violations fail normal builds. Server behavior costs more to test but is verified against
the implementation that production uses; coverage remains a diagnostic rather than a vanity target.
