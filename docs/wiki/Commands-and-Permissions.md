# Commands and Permissions

The public root command is `/spectraevents`.

## Everyday operations

```text
/spectraevents status
/spectraevents doctor
/spectraevents event start <definition> [location <name>] [key=value ...]
/spectraevents event inspect <instance-id>
/spectraevents event stop <instance-id>
/spectraevents definition list
/spectraevents definition validate
/spectraevents definition reload
/spectraevents template list
/spectraevents template install <airdrop|boss-portal|meteor|metin|pinata>
/spectraevents template update <airdrop|boss-portal|meteor|metin|pinata>
/spectraevents template remove <airdrop|boss-portal|meteor|metin|pinata>
/spectraevents schedule reload
/spectraevents rewards list
/spectraevents rewards claim <claim-id>
```

Interrupted `DELIVERING` claims are never silently reissued. An operator must inspect evidence and
choose the explicit rewards reconciliation command for `mark-delivered` or `return-pending`.

Permission nodes are namespaced below `spectraevents`. Verify the exact command help and generated
plugin metadata for the installed version; the beta command contract may still change. Paper offers
an inventory GUI for selected administration flows. Equivalent Spigot operator UX is planned but is
not currently complete.
