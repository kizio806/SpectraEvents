package io.github.kizio806.spectraevents.adapter.blockbench;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.github.kizio806.spectraevents.core.visual.animation.Easing;
import io.github.kizio806.spectraevents.core.visual.animation.LoopMode;
import io.github.kizio806.spectraevents.core.visual.model.EulerRotation;
import io.github.kizio806.spectraevents.core.visual.model.Vector3;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.Test;

class BlockbenchJsonSupportTest {

  @Test
  void testAssertJsonDepth() {
    BlockbenchJsonSupport.assertJsonDepth("{\"a\": {\"b\": [1, 2]}}", 10);
    assertThrows(
        IllegalArgumentException.class,
        () -> BlockbenchJsonSupport.assertJsonDepth("{\"a\": {\"b\": 1}}", 1));
  }

  @Test
  void testParseObject() {
    JsonObject obj = BlockbenchJsonSupport.parseObject("{\"hello\":\"world\"}", "test.json");
    assertEquals("world", obj.get("hello").getAsString());

    assertThrows(
        IllegalArgumentException.class,
        () -> BlockbenchJsonSupport.parseObject("not_json", "test.json"));
  }

  @Test
  void testRequiredAndOptionalPrimitives() {
    JsonObject obj = new JsonObject();
    obj.addProperty("str", "value");
    obj.addProperty("num", 42);
    obj.addProperty("dbl", 3.5);

    assertEquals("value", BlockbenchJsonSupport.requiredString(obj, "str", "root"));
    assertEquals(42, BlockbenchJsonSupport.requiredInt(obj, "num", "root"));
    assertEquals(3.5, BlockbenchJsonSupport.requiredFiniteDouble(obj, "dbl", "root"), 0.001);

    assertEquals("fallback", BlockbenchJsonSupport.optionalString(obj, "missing", "fallback"));
    assertEquals(10, BlockbenchJsonSupport.optionalInt(obj, "missing", 10, "root"));
    assertEquals(1.5f, BlockbenchJsonSupport.optionalFiniteFloat(obj, "missing", 1.5f, "root"));

    assertThrows(
        IllegalArgumentException.class,
        () -> BlockbenchJsonSupport.requiredString(obj, "missing", "root"));
    assertThrows(
        IllegalArgumentException.class,
        () -> BlockbenchJsonSupport.requiredInt(obj, "missing", "root"));
    assertThrows(
        IllegalArgumentException.class,
        () -> BlockbenchJsonSupport.requiredFiniteDouble(obj, "missing", "root"));
  }

  @Test
  void testVectorAndRotation() {
    JsonObject obj = new JsonObject();
    JsonArray arr = new JsonArray();
    arr.add(1.0f);
    arr.add(2.0f);
    arr.add(3.0f);
    obj.add("pos", arr);

    Vector3 vec = BlockbenchJsonSupport.requiredVector(obj, "pos", "root");
    assertEquals(new Vector3(1.0f, 2.0f, 3.0f), vec);
    EulerRotation rot = BlockbenchJsonSupport.toEuler(vec);
    assertEquals(1.0f, rot.pitchX());
    assertEquals(2.0f, rot.yawY());
    assertEquals(3.0f, rot.rollZ());

    Vector3 opt = BlockbenchJsonSupport.optionalVector(obj, "missing", Vector3.ZERO, "root");
    assertEquals(Vector3.ZERO, opt);

    assertThrows(
        IllegalArgumentException.class,
        () -> BlockbenchJsonSupport.requiredVector(obj, "missing", "root"));
  }

  @Test
  void testRequiredUv() {
    JsonObject face = new JsonObject();
    JsonArray uvArr = new JsonArray();
    uvArr.add(0.0);
    uvArr.add(0.0);
    uvArr.add(16.0);
    uvArr.add(16.0);
    face.add("uv", uvArr);

    List<Double> uv = BlockbenchJsonSupport.requiredUv(face, "face");
    assertEquals(List.of(0.0, 0.0, 16.0, 16.0), uv);

    JsonObject badFace = new JsonObject();
    assertThrows(
        IllegalArgumentException.class, () -> BlockbenchJsonSupport.requiredUv(badFace, "face"));
  }

  @Test
  void testDataPointVector() {
    JsonObject kf = new JsonObject();
    JsonArray points = new JsonArray();
    JsonObject pt = new JsonObject();
    pt.addProperty("x", 5.0f);
    pt.addProperty("y", 10.0f);
    pt.addProperty("z", 15.0f);
    points.add(pt);
    kf.add("data_points", points);

    Vector3 vec = BlockbenchJsonSupport.dataPointVector(kf, "keyframe");
    assertEquals(new Vector3(5.0f, 10.0f, 15.0f), vec);

    JsonObject emptyKf = new JsonObject();
    emptyKf.add("data_points", new JsonArray());
    assertThrows(
        IllegalArgumentException.class,
        () -> BlockbenchJsonSupport.dataPointVector(emptyKf, "empty"));
  }

  @Test
  void testParseLoopModeAndEasing() {
    assertEquals(LoopMode.ONCE, BlockbenchJsonSupport.parseLoopMode("once"));
    assertEquals(LoopMode.LOOP, BlockbenchJsonSupport.parseLoopMode("loop"));
    assertEquals(LoopMode.PING_PONG, BlockbenchJsonSupport.parseLoopMode("pingpong"));
    assertEquals(LoopMode.PING_PONG, BlockbenchJsonSupport.parseLoopMode("ping_pong"));
    assertThrows(
        IllegalArgumentException.class, () -> BlockbenchJsonSupport.parseLoopMode("invalid"));

    assertEquals(Easing.LINEAR, BlockbenchJsonSupport.parseEasing("linear"));
    assertEquals(Easing.STEP, BlockbenchJsonSupport.parseEasing("step"));
    assertEquals(Easing.EASE_IN_OUT_CUBIC, BlockbenchJsonSupport.parseEasing("catmullrom"));
    assertThrows(
        IllegalArgumentException.class, () -> BlockbenchJsonSupport.parseEasing("unknown"));
  }

  @Test
  void testDecodeUtf8AndSha256() {
    byte[] validUtf8 = "test message".getBytes(StandardCharsets.UTF_8);
    assertEquals("test message", BlockbenchJsonSupport.decodeUtf8(validUtf8, "entry.txt"));

    String hash = BlockbenchJsonSupport.sha256(validUtf8);
    assertNotNull(hash);
    assertEquals(64, hash.length());

    byte[] invalidUtf8 = new byte[] {(byte) 0xFF, (byte) 0xC0};
    assertThrows(
        IllegalArgumentException.class,
        () -> BlockbenchJsonSupport.decodeUtf8(invalidUtf8, "bad.txt"));
  }

  @Test
  void testObjectAndArrayValidation() {
    JsonObject parent = new JsonObject();
    JsonObject child = new JsonObject();
    child.addProperty("key", "val");
    parent.add("child", child);
    JsonArray arr = new JsonArray();
    arr.add(1);
    parent.add("items", arr);

    assertEquals(child, BlockbenchJsonSupport.requiredObject(parent, "child", "parent"));
    assertEquals(arr, BlockbenchJsonSupport.requiredArray(parent, "items", "parent"));

    assertThrows(
        IllegalArgumentException.class,
        () -> BlockbenchJsonSupport.requiredObject(parent, "missing", "parent"));
    assertThrows(
        IllegalArgumentException.class,
        () -> BlockbenchJsonSupport.requiredArray(parent, "missing", "parent"));
    assertThrows(
        IllegalArgumentException.class, () -> BlockbenchJsonSupport.requireObject(arr, "not_obj"));
  }

  @Test
  void testVectorAndFloatValidationErrors() {
    JsonObject obj = new JsonObject();
    obj.addProperty("bad_float", 25.0); // Outside [-16, 16]
    assertThrows(
        IllegalArgumentException.class,
        () -> BlockbenchJsonSupport.optionalFiniteFloat(obj, "bad_float", 0f, "root"));

    JsonArray shortVec = new JsonArray();
    shortVec.add(1.0);
    shortVec.add(2.0);
    obj.add("short", shortVec);
    assertThrows(
        IllegalArgumentException.class, () -> BlockbenchJsonSupport.vector(shortVec, "short"));
  }

  @Test
  void testDataPointComponentBranches() {
    JsonObject point = new JsonObject();
    point.addProperty("x", 1.0f);
    assertEquals(1.0f, BlockbenchJsonSupport.dataPointComponent(point, "x", "path"), 0.001f);

    assertThrows(
        IllegalArgumentException.class,
        () -> BlockbenchJsonSupport.dataPointComponent(point, "missing", "path"));

    JsonObject infinitePoint = new JsonObject();
    infinitePoint.addProperty("x", Float.POSITIVE_INFINITY);
    assertThrows(
        IllegalArgumentException.class,
        () -> BlockbenchJsonSupport.dataPointComponent(infinitePoint, "x", "path"));

    JsonObject nonNumericPoint = new JsonObject();
    nonNumericPoint.addProperty("x", "not_a_number");
    assertThrows(
        IllegalArgumentException.class,
        () -> BlockbenchJsonSupport.dataPointComponent(nonNumericPoint, "x", "path"));
  }

  @Test
  void testAdditionalLoopModesAndPrimitives() {
    assertEquals(LoopMode.ONCE, BlockbenchJsonSupport.parseLoopMode(""));
    assertEquals(LoopMode.PING_PONG, BlockbenchJsonSupport.parseLoopMode("ping-pong"));

    JsonObject obj = new JsonObject();
    obj.addProperty("str", "hello");
    obj.addProperty("num", 123);
    obj.addProperty("nan", Double.NaN);

    assertEquals("hello", BlockbenchJsonSupport.optionalString(obj, "str", "default"));
    assertEquals(123, BlockbenchJsonSupport.optionalInt(obj, "num", 0, "path"));

    assertThrows(
        IllegalArgumentException.class,
        () -> BlockbenchJsonSupport.requiredFiniteDouble(obj, "nan", "path"));
  }
}
