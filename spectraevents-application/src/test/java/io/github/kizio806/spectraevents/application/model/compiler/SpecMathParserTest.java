package io.github.kizio806.spectraevents.application.model.compiler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.kizio806.spectraevents.application.config.validation.ValidationDiagnostic;
import io.github.kizio806.spectraevents.core.visual.model.EulerRotation;
import io.github.kizio806.spectraevents.core.visual.model.Quaternion;
import io.github.kizio806.spectraevents.core.visual.model.Vector3;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class SpecMathParserTest {

  @Test
  void testParseVector3() {
    List<ValidationDiagnostic> diagnostics = new ArrayList<>();
    Vector3 def = Vector3.of(1.0f, 2.0f, 3.0f);

    // Null and empty
    assertEquals(def, SpecMathParser.parseVector3(null, def, "pos", diagnostics));
    assertEquals(def, SpecMathParser.parseVector3(List.of(), def, "pos", diagnostics));
    assertTrue(diagnostics.isEmpty());

    // Invalid dimension
    assertEquals(def, SpecMathParser.parseVector3(List.of(1.0f, 2.0f), def, "pos", diagnostics));
    assertEquals(1, diagnostics.size());
    assertEquals("INVALID_VECTOR3", diagnostics.get(0).code());

    // Valid
    diagnostics.clear();
    Vector3 parsed =
        SpecMathParser.parseVector3(List.of(4.0f, 5.0f, 6.0f), def, "pos", diagnostics);
    assertEquals(Vector3.of(4.0f, 5.0f, 6.0f), parsed);
    assertTrue(diagnostics.isEmpty());

    // Invalid float values (NaN)
    diagnostics.clear();
    Vector3 nanParsed =
        SpecMathParser.parseVector3(List.of(Float.NaN, 0.0f, 0.0f), def, "pos", diagnostics);
    assertEquals(def, nanParsed);
    assertEquals(1, diagnostics.size());
    assertEquals("INVALID_VECTOR3_VALUES", diagnostics.get(0).code());
  }

  @Test
  void testParseEuler() {
    List<ValidationDiagnostic> diagnostics = new ArrayList<>();

    // Null and wrong size (3-arg overload)
    assertEquals(EulerRotation.ZERO, SpecMathParser.parseEuler(null, "rot", diagnostics));
    assertEquals("INVALID_EULER", diagnostics.get(0).code());

    diagnostics.clear();
    assertEquals(EulerRotation.ZERO, SpecMathParser.parseEuler(List.of(1.0f), "rot", diagnostics));
    assertEquals("INVALID_EULER", diagnostics.get(0).code());

    // 4-arg overload with custom error code
    diagnostics.clear();
    assertEquals(
        EulerRotation.ZERO,
        SpecMathParser.parseEuler(List.of(1.0f, 2.0f), "rot", diagnostics, "CUSTOM_ERR"));
    assertEquals("CUSTOM_ERR", diagnostics.get(0).code());

    // Valid
    diagnostics.clear();
    EulerRotation valid =
        SpecMathParser.parseEuler(List.of(10.0f, 20.0f, 30.0f), "rot", diagnostics);
    assertEquals(new EulerRotation(10.0f, 20.0f, 30.0f), valid);
    assertTrue(diagnostics.isEmpty());

    // Invalid float values (Infinity)
    diagnostics.clear();
    EulerRotation invalid =
        SpecMathParser.parseEuler(
            List.of(Float.POSITIVE_INFINITY, 0.0f, 0.0f), "rot", diagnostics, "CUSTOM_ERR");
    assertEquals(EulerRotation.ZERO, invalid);
    assertEquals("CUSTOM_ERR_VALUES", diagnostics.get(0).code());
  }

  @Test
  void testParseQuaternion() {
    List<ValidationDiagnostic> diagnostics = new ArrayList<>();

    // Null and wrong size (3-arg overload)
    assertEquals(Quaternion.IDENTITY, SpecMathParser.parseQuaternion(null, "quat", diagnostics));
    assertEquals("INVALID_QUATERNION", diagnostics.get(0).code());

    diagnostics.clear();
    assertEquals(
        Quaternion.IDENTITY,
        SpecMathParser.parseQuaternion(List.of(1.0f, 2.0f, 3.0f), "quat", diagnostics));
    assertEquals("INVALID_QUATERNION", diagnostics.get(0).code());

    // 4-arg overload with custom error code
    diagnostics.clear();
    assertEquals(
        Quaternion.IDENTITY,
        SpecMathParser.parseQuaternion(List.of(1.0f), "quat", diagnostics, "CUSTOM_QUAT_ERR"));
    assertEquals("CUSTOM_QUAT_ERR", diagnostics.get(0).code());

    // Valid
    diagnostics.clear();
    Quaternion valid =
        SpecMathParser.parseQuaternion(List.of(0.0f, 0.0f, 0.0f, 1.0f), "quat", diagnostics);
    assertEquals(Quaternion.IDENTITY, valid);
    assertTrue(diagnostics.isEmpty());

    // Invalid float values (NaN)
    diagnostics.clear();
    Quaternion invalid =
        SpecMathParser.parseQuaternion(
            List.of(Float.NaN, 0.0f, 0.0f, 1.0f), "quat", diagnostics, "CUSTOM_QUAT_ERR");
    assertEquals(Quaternion.IDENTITY, invalid);
    assertEquals("CUSTOM_QUAT_ERR_VALUES", diagnostics.get(0).code());
  }
}
