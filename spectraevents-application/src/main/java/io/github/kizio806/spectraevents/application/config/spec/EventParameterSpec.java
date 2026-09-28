package io.github.kizio806.spectraevents.application.config.spec;

import java.math.BigDecimal;
import java.util.Objects;

/** YAML-owned declaration of one scalar operator setting. */
public record EventParameterSpec(
    String name,
    EventParameterType type,
    Object defaultValue,
    BigDecimal minimum,
    BigDecimal maximum,
    BigDecimal step,
    boolean guiEditable) {
  public EventParameterSpec {
    if (name == null || !name.matches("[a-z][a-z0-9-]{0,63}")) {
      throw new IllegalArgumentException(
          "Parameter name must use lowercase letters, digits and dashes");
    }
    Objects.requireNonNull(type, "type");
    Objects.requireNonNull(defaultValue, "defaultValue");
    if ((minimum == null) != (maximum == null)) {
      throw new IllegalArgumentException("minimum and maximum must be declared together");
    }
    if (minimum != null && minimum.compareTo(maximum) > 0) {
      throw new IllegalArgumentException("minimum must not exceed maximum");
    }
    if (guiEditable && (step == null || step.signum() <= 0)) {
      throw new IllegalArgumentException("GUI-editable parameters require a positive step");
    }
  }
}
