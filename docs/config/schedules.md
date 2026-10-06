# Schedules

`plugins/SpectraEvents/schedules.yml` starts definitions at cron times. It is a versioned, separate
file so that an invalid entry can be reported without disabling valid sibling schedules.

```yaml
schema-version: 1
schedules:
  - id: weekday-meteor
    definition: meteor
    cron: "0 20 * * 1-5"
    timezone: Europe/Warsaw
    world: world
    x: 0
    y: 100
    z: 0
    parameters:
      health: 250
      lock-duration: 15m
```

Required keys are `id`, `definition`, `cron`, `timezone`, `world`, `x`, `y` and `z`. `timezone` is
an IANA zone such as `Europe/Warsaw`; do not rely on the server's default zone. The scheduler checks
at minute resolution and does not replay missed runs after a restart.

`parameters` is optional. Its keys and values must be scalar YAML values, and they override only the
definition compiled for that one scheduled start. They do not modify the definition file or Paper's
saved GUI override.

After editing, use `/spectraevents schedule reload`; `/spectraevents schedule list` displays the
loaded entries. Correct every `schedules.yml:path` diagnostic before treating a schedule as active.
