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
Animations are triggered via the `play-animation` Action. The engine calculates the required `interpolation_duration` and updates the display entity data over time to create smooth movement without needing to send a packet every single tick.
