# ADR 0001: Hexagonal core

## Status

Accepted

## Context

SpectraEvents must express reusable event behavior independently of any Minecraft server platform.

## Decision

Use ports and adapters with dependency direction `platform -> application -> core`. Core,
application, and public API may not depend on Bukkit, Paper, NMS, or CraftBukkit. Infrastructure
implements ports defined by inner layers.

## Consequences

Domain behavior is testable without a server and can support multiple platform adapters. Adapter
composition is explicit, at the cost of maintaining clear module boundaries.
