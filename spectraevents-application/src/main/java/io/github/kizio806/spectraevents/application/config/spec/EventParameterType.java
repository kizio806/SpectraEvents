package io.github.kizio806.spectraevents.application.config.spec;

/** Scalar types which can safely be exposed as operator overrides. */
public enum EventParameterType {
  INTEGER,
  DECIMAL,
  DURATION,
  BOOLEAN;

  public static EventParameterType parse(String value) {
    if (value == null) {
      throw new IllegalArgumentException("Event parameter type cannot be null");
    }
    try {
      return valueOf(value.trim().toUpperCase(java.util.Locale.ROOT));
    } catch (IllegalArgumentException exception) {
      throw new IllegalArgumentException("Unsupported event parameter type: " + value, exception);
    }
  }
}
