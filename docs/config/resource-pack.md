# Resource Pack Configuration

Resource-pack compilation and player delivery are disabled in the current beta. There is no active
`resource-pack` configuration contract, no automatic Modrinth lookup, and no supported manual-URL
download path in either distribution.

The disabled code remains an implementation area for a later milestone. Do not add undocumented
keys to `config.yml`; asset commands report that the feature is unavailable.

Enabling this feature requires a verified ZIP builder, bounded HTTPS delivery, checksum validation,
cache policy, and tested accept/decline/failure handling.
