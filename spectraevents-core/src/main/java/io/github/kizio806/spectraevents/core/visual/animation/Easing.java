package io.github.kizio806.spectraevents.core.visual.animation;

/** Easing curves for keyframe parameter interpolation. */
public enum Easing {
  LINEAR {
    @Override
    public double evaluate(double t) {
      return clamp(t);
    }
  },
  EASE_IN_QUAD {
    @Override
    public double evaluate(double t) {
      t = clamp(t);
      return t * t;
    }
  },
  EASE_OUT_QUAD {
    @Override
    public double evaluate(double t) {
      t = clamp(t);
      return t * (2.0 - t);
    }
  },
  EASE_IN_OUT_QUAD {
    @Override
    public double evaluate(double t) {
      t = clamp(t);
      return t < 0.5 ? 2.0 * t * t : -1.0 + (4.0 - 2.0 * t) * t;
    }
  },
  EASE_IN_CUBIC {
    @Override
    public double evaluate(double t) {
      t = clamp(t);
      return t * t * t;
    }
  },
  EASE_OUT_CUBIC {
    @Override
    public double evaluate(double t) {
      t = clamp(t);
      double f = t - 1.0;
      return f * f * f + 1.0;
    }
  },
  EASE_IN_OUT_CUBIC {
    @Override
    public double evaluate(double t) {
      t = clamp(t);
      return t < 0.5 ? 4.0 * t * t * t : (t - 1.0) * (2.0 * t - 2.0) * (2.0 * t - 2.0) + 1.0;
    }
  },
  EASE_IN_SINE {
    @Override
    public double evaluate(double t) {
      t = clamp(t);
      return 1.0 - Math.cos((t * Math.PI) / 2.0);
    }
  },
  EASE_OUT_SINE {
    @Override
    public double evaluate(double t) {
      t = clamp(t);
      return Math.sin((t * Math.PI) / 2.0);
    }
  },
  EASE_IN_OUT_SINE {
    @Override
    public double evaluate(double t) {
      t = clamp(t);
      return -(Math.cos(Math.PI * t) - 1.0) / 2.0;
    }
  };

  public abstract double evaluate(double t);

  private static double clamp(double t) {
    if (t <= 0.0) return 0.0;
    if (t >= 1.0) return 1.0;
    return t;
  }
}
