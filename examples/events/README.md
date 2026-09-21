# Verified Event Examples

`meteor.yml`, `airdrop.yml`, `metin.yml`, `pinata.yml`, and `boss-portal.yml` are schema-v1 authoring examples. They use the same
model, animation, phase, trigger, condition, action, reward, recovery, and cleanup contracts as
server definitions. The shipped resource copies are loaded by automated tests on every build.

| File | Main mechanics |
| :--- | :--- |
| `meteor.yml` | model + named animation, timer phases, health, mob waves, loot, cleanup |
| `airdrop.yml` | model + named animation, timed unlock, interaction claim, rewards, UI cleanup |
| `metin.yml` | model + looping animation, health damage, threshold phases, boss and loot cleanup |
| `pinata.yml` | model interaction, equal-value hit counter, completion trigger and cleanup |
| `boss-portal.yml` | model + opening animation, timer, spawned boss, death trigger and cleanup |

Copy one of these files, change its `id`, and then follow [Create Your First Event](../../docs/guides/create-your-first-event.md).

`vault.yml` remains a design draft for future schema capabilities. It is intentionally not advertised
as a runnable v1 example until its fields are implemented and tested.
