# Animation Specification

Animations define how `Model Parts` move over time.

## Design
SpectraEvents does not aim to replace full skeletal animation systems. Instead, it provides simple linear and eased property tracks utilizing the native interpolation capabilities of Minecraft Display Entities.

## Structure
```yaml
animations:
  meteor_spin:
    duration: 20s
    loop: LOOP
    recovery: RESUME
    tracks:
      main_body:
        rotation:
          - at: 0s
            value: [0, 0, 0]
          - at: 20s
            value: [0, 360, 0]
```

## Properties
- `duration`: Total animation length, for example `1s` or `500ms`.
- `loop`: `ONCE`, `LOOP`, or `PING_PONG`.
- `recovery`: `RESUME`, `RESTART`, or `STOP` after a restart.
- `tracks`: Map of model part IDs to `translation`, `rotation`, or `scale` keyframes.
- `at`: Keyframe time. `value` is a three-number vector; rotation values are Euler degrees.

## Execution
Animations are triggered explicitly with the `play_animation` action after the model has been
spawned. The engine looks up the compiled animation registered for the model, starts a playback
owned by the event, and stops it automatically before the model is removed.

```yaml
phases:
  active:
    on-enter:
      - type: spawn_model
        model: meteor
      - type: play_animation
        model: meteor
        animation: meteor_spin
        loop: LOOP
        speed: 1.0
```

The action also accepts the `play-animation` spelling for compatibility. Use the underscore form
in new definitions. `model` may be omitted when an event owns only one model. The engine calculates
the required `interpolation_duration` and updates display entity data over time to create smooth
movement without sending a packet every server tick.
