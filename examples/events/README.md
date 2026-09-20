# Verified Event Examples

`meteor.yml`, `airdrop.yml`, and `metin.yml` are schema-v1 authoring examples. They use the same
model, animation, phase, trigger, condition, action, reward, recovery, and cleanup contracts as
server definitions. The shipped resource copies are loaded by automated tests on every build.

| File | Main mechanics |
| :--- | :--- |
| `meteor.yml` | model + named animation, timer phases, health, mob waves, loot, cleanup |
| `airdrop.yml` | model + named animation, timed unlock, interaction claim, rewards, UI cleanup |
| `metin.yml` | model + looping animation, health damage, threshold phases, boss and loot cleanup |

Copy one of these files, change its `id`, and then follow [Create Your First Event](../../docs/guides/create-your-first-event.md).

The remaining files in this directory (`boss-portal.yml`, `pinata.yml`, and `vault.yml`) are
design drafts for future schema capabilities. They are intentionally not advertised as runnable
v1 examples until their fields are implemented and tested.
