# ADR 0002: Versioned Paper adapters

## Status

Accepted

## Context

The initial target is Paper 26.2 on Java 25, while future support may include older Paper lines that
run on Java 21.

## Decision

Keep stable cross-version Paper code in `platforms/paper-common` and isolate version-specific behavior
in modules such as `platforms/paper-26_2`. Platform-neutral modules compile with `--release 21`; the
Paper 26.2 adapter compiles with `--release 25`.

## Consequences

New Paper versions can be added without rebuilding the core around scattered version checks. Code may
move out of `paper-common` when real compatibility differences are discovered.
