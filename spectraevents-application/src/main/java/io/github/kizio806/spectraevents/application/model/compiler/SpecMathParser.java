package io.github.kizio806.spectraevents.application.model.compiler;

import io.github.kizio806.spectraevents.application.config.validation.ValidationDiagnostic;
import io.github.kizio806.spectraevents.application.config.validation.ValidationDiagnostic.Severity;
import io.github.kizio806.spectraevents.core.visual.model.EulerRotation;
import io.github.kizio806.spectraevents.core.visual.model.Quaternion;
import io.github.kizio806.spectraevents.core.visual.model.Vector3;
import java.util.List;

/** Shared utility for parsing and validating 3D math primitives from YAML/JSON specs. */
public final class SpecMathParser {
  private SpecMathParser() {}

  public static Vector3 parseVector3(
      List<Float> list, Vector3 defaultValue, String path, List<ValidationDiagnostic> diagnostics) {
    if (list == null || list.isEmpty()) {
      return defaultValue;
    }
    if (list.size() != 3) {
      diagnostics.add(
          new ValidationDiagnostic(
              Severity.ERROR,
              "INVALID_VECTOR3",
              path,
              "Vector3 must contain 3 float elements [x, y, z]"));
      return defaultValue;
    }
    try {
      return Vector3.of(list.get(0), list.get(1), list.get(2));
    } catch (IllegalArgumentException e) {
      diagnostics.add(
          new ValidationDiagnostic(Severity.ERROR, "INVALID_VECTOR3_VALUES", path, e.getMessage()));
      return defaultValue;
    }
  }

  public static EulerRotation parseEuler(
      List<Float> list, String path, List<ValidationDiagnostic> diagnostics) {
    return parseEuler(list, path, diagnostics, "INVALID_EULER");
  }

  public static EulerRotation parseEuler(
      List<Float> list, String path, List<ValidationDiagnostic> diagnostics, String errorCode) {
    if (list == null || list.size() != 3) {
      diagnostics.add(
          new ValidationDiagnostic(
              Severity.ERROR,
              errorCode,
              path,
              "Euler rotation must contain 3 float elements [pitch, yaw, roll]"));
      return EulerRotation.ZERO;
    }
    try {
      return new EulerRotation(list.get(0), list.get(1), list.get(2));
    } catch (IllegalArgumentException e) {
      diagnostics.add(
          new ValidationDiagnostic(Severity.ERROR, errorCode + "_VALUES", path, e.getMessage()));
      return EulerRotation.ZERO;
    }
  }

  public static Quaternion parseQuaternion(
      List<Float> list, String path, List<ValidationDiagnostic> diagnostics) {
    return parseQuaternion(list, path, diagnostics, "INVALID_QUATERNION");
  }

  public static Quaternion parseQuaternion(
      List<Float> list, String path, List<ValidationDiagnostic> diagnostics, String errorCode) {
    if (list == null || list.size() != 4) {
      diagnostics.add(
          new ValidationDiagnostic(
              Severity.ERROR,
              errorCode,
              path,
              "Quaternion must contain 4 float elements [x, y, z, w]"));
      return Quaternion.IDENTITY;
    }
    try {
      return Quaternion.of(list.get(0), list.get(1), list.get(2), list.get(3));
    } catch (IllegalArgumentException e) {
      diagnostics.add(
          new ValidationDiagnostic(Severity.ERROR, errorCode + "_VALUES", path, e.getMessage()));
      return Quaternion.IDENTITY;
    }
  }
}
