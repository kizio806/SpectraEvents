# Animation Specification

Animations define how `Model Parts` move over time.

## Design
SpectraEvents does not aim to replace full skeletal animation systems. Instead, it provides simple linear and eased property tracks utilizing the native interpolation capabilities of Minecraft Display Entities.

## Structure
```yaml
animations:
  meteor_spin:
    length: 20s
    loop: true
    tracks:
      - part: main_body
        property: rotation
        easing: LINEAR
        keyframes:
          - time: 0s
            value: [0, 0]
          - time: 20s
            value: [360, 360]
```

## Properties
- `property`: Can be `rotation`, `translation` (offset), or `scale`.
- `easing`: Supported easings include `LINEAR`, `EASE_IN`, `EASE_OUT`, `EASE_IN_OUT`.

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
