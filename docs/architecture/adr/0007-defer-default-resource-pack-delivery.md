# ADR 0007: Defer Default Resource-Pack Delivery

**Status:** Accepted

## Context

ADR 0006 required default Modrinth delivery. The configured project did not resolve on a fresh
installation, causing a server either to fail startup or to claim visual delivery without a usable
descriptor.

## Decision

This ADR supersedes ADR 0006 for the default configuration. Delivery is disabled by default. An
administrator may enable only a verified HTTPS ZIP and SHA-1, or a real pinned Modrinth descriptor
once it exists and has release evidence. A failed descriptor must never be reported as ready.

## Consequences

Fresh installation and diagnostics remain usable. A server that needs client visuals must configure
and verify delivery before advertising the visual experience or cutting a release.
