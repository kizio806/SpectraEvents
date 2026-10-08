# 0005. Defer the Public API Module

* Status: Accepted
* Date: 2026-09-19

## Context

The `spectraevents-api` module exported only package documentation. It had no callable contract,
runtime registration mechanism, compatibility policy, or tests. Shipping the empty module implied a
supported addon API that did not exist.

## Decision

Remove `spectraevents-api` from the build. Core and application continue to expose only internal Java
contracts. A public API may return after a concrete addon use case establishes the smallest stable
surface, lifecycle/registration semantics, versioning policy, and compatibility tests.

## Consequences

* Addon developers are not given a false compatibility promise during beta.
* Core continues to depend only on the Java standard library.
* Reintroducing a public API requires a superseding ADR and executable compatibility tests.
