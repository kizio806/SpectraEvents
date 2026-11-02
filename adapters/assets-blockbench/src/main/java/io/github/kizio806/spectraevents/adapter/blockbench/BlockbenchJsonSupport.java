package io.github.kizio806.spectraevents.adapter.blockbench;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.github.kizio806.spectraevents.core.visual.animation.Easing;
import io.github.kizio806.spectraevents.core.visual.animation.LoopMode;
import io.github.kizio806.spectraevents.core.visual.model.EulerRotation;
import io.github.kizio806.spectraevents.core.visual.model.Vector3;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Strict JSON extraction and normalization utilities for parsing Blockbench project definitions.
 */
final class BlockbenchJsonSupport {
  static final int MAX_JSON_DEPTH = 64;

  private BlockbenchJsonSupport() {}

  static void assertJsonDepth(String json, int maxDepth) {
    int depth = 0;
    boolean inString = false;
    boolean escaping = false;
    for (int index = 0; index < json.length(); index++) {
      char value = json.charAt(index);
      if (inString) {
        if (escaping) {
          escaping = false;
        } else if (value == '\\') {
          escaping = true;
        } else if (value == '"') {
          inString = false;
        }
      } else if (value == '"') {
        inString = true;
      } else if (value == '{' || value == '[') {
        depth++;
        if (depth > maxDepth) {
          throw new IllegalArgumentException("JSON nesting exceeds maximum depth of " + maxDepth);
        }
      } else if (value == '}' || value == ']') {
        depth--;
      }
    }
  }

  static JsonObject parseObject(String json, String path) {
    try {
      return requireObject(JsonParser.parseString(json), path);
    } catch (RuntimeException exception) {
      throw new IllegalArgumentException("Invalid JSON in " + path, exception);
    }
  }

  static JsonObject requiredObject(JsonObject parent, String name, String path) {
    if (!parent.has(name)) {
      throw new IllegalArgumentException(path + " is missing required field " + name);
    }
    return requireObject(parent.get(name), path + "." + name);
  }

  static JsonArray requiredArray(JsonObject parent, String name, String path) {
    if (!parent.has(name) || !parent.get(name).isJsonArray()) {
      throw new IllegalArgumentException(path + "." + name + " must be an array");
    }
    return parent.getAsJsonArray(name);
  }

  static JsonObject requireObject(JsonElement element, String path) {
    if (element == null || !element.isJsonObject()) {
      throw new IllegalArgumentException(path + " must be an object");
    }
    return element.getAsJsonObject();
  }

  static String requiredString(JsonObject object, String name, String path) {
    if (!object.has(name)
        || !object.get(name).isJsonPrimitive()
        || !object.get(name).getAsJsonPrimitive().isString()
        || object.get(name).getAsString().isBlank()) {
      throw new IllegalArgumentException(path + "." + name + " must be a non-empty string");
    }
    return object.get(name).getAsString();
  }

  static String optionalString(JsonObject object, String name, String defaultValue) {
    if (!object.has(name)) {
      return defaultValue;
    }
    if (!object.get(name).isJsonPrimitive() || !object.get(name).getAsJsonPrimitive().isString()) {
      throw new IllegalArgumentException(name + " must be a string");
    }
    return object.get(name).getAsString();
  }

  static int requiredInt(JsonObject object, String name, String path) {
    if (!object.has(name)) {
      throw new IllegalArgumentException(path + "." + name + " must be an integer");
    }
    return optionalInt(object, name, 0, path);
  }

  static int optionalInt(JsonObject object, String name, int defaultValue, String path) {
    if (!object.has(name)) {
      return defaultValue;
    }
    try {
      return object.get(name).getAsInt();
    } catch (RuntimeException exception) {
      throw new IllegalArgumentException(path + "." + name + " must be an integer", exception);
    }
  }

  static double requiredFiniteDouble(JsonObject object, String name, String path) {
    if (!object.has(name)) {
      throw new IllegalArgumentException(path + "." + name + " must be a number");
    }
    try {
      double value = object.get(name).getAsDouble();
      if (!Double.isFinite(value)) {
        throw new IllegalArgumentException(path + "." + name + " must be finite");
      }
      return value;
    } catch (RuntimeException exception) {
      throw new IllegalArgumentException(path + "." + name + " must be a number", exception);
    }
  }

  static float optionalFiniteFloat(
      JsonObject object, String name, float defaultValue, String path) {
    if (!object.has(name)) {
      return defaultValue;
    }
    double value = requiredFiniteDouble(object, name, path);
    if (value < -16.0 || value > 16.0) {
      throw new IllegalArgumentException(path + "." + name + " is outside supported range");
    }
    return (float) value;
  }

  static Vector3 requiredVector(JsonObject object, String name, String path) {
    if (!object.has(name)) {
      throw new IllegalArgumentException(path + "." + name + " must be a three-number array");
    }
    return vector(object.get(name), path + "." + name);
  }

  static Vector3 optionalVector(JsonObject object, String name, Vector3 defaultValue, String path) {
    return object.has(name) ? vector(object.get(name), path + "." + name) : defaultValue;
  }

  static Vector3 vector(JsonElement element, String path) {
    if (!element.isJsonArray() || element.getAsJsonArray().size() != 3) {
      throw new IllegalArgumentException(path + " must be a three-number array");
    }
    JsonArray values = element.getAsJsonArray();
    try {
      float x = values.get(0).getAsFloat();
      float y = values.get(1).getAsFloat();
      float z = values.get(2).getAsFloat();
      if (!Float.isFinite(x) || !Float.isFinite(y) || !Float.isFinite(z)) {
        throw new IllegalArgumentException(path + " must contain finite numbers");
      }
      return new Vector3(x, y, z);
    } catch (RuntimeException exception) {
      throw new IllegalArgumentException(path + " must be a three-number array", exception);
    }
  }

  static EulerRotation toEuler(Vector3 vector) {
    return new EulerRotation(vector.x(), vector.y(), vector.z());
  }

  static List<Double> requiredUv(JsonObject face, String path) {
    if (!face.has("uv") || !face.get("uv").isJsonArray() || face.getAsJsonArray("uv").size() != 4) {
      throw new IllegalArgumentException(path + ".uv must be a four-number array");
    }
    List<Double> uv = new ArrayList<>(4);
    for (JsonElement value : face.getAsJsonArray("uv")) {
      try {
        double coordinate = value.getAsDouble();
        if (!Double.isFinite(coordinate)) {
          throw new IllegalArgumentException(path + ".uv must contain finite numbers");
        }
        uv.add(coordinate);
      } catch (RuntimeException exception) {
        throw new IllegalArgumentException(path + ".uv must be a four-number array", exception);
      }
    }
    return List.copyOf(uv);
  }

  static Vector3 dataPointVector(JsonObject keyframe, String path) {
    JsonArray points = requiredArray(keyframe, "data_points", path);
    if (points.isEmpty()) {
      throw new IllegalArgumentException(path + ".data_points must not be empty");
    }
    JsonObject point = requireObject(points.get(0), path + ".data_points[0]");
    return new Vector3(
        dataPointComponent(point, "x", path),
        dataPointComponent(point, "y", path),
        dataPointComponent(point, "z", path));
  }

  static float dataPointComponent(JsonObject point, String component, String path) {
    if (!point.has(component)) {
      throw new IllegalArgumentException(path + ".data_points[0]." + component + " is required");
    }
    try {
      float value = point.get(component).getAsFloat();
      if (!Float.isFinite(value)) {
        throw new IllegalArgumentException(
            path + ".data_points[0]." + component + " must be finite");
      }
      return value;
    } catch (RuntimeException exception) {
      throw new IllegalArgumentException(
          path + ".data_points[0]." + component + " must be numeric", exception);
    }
  }

  static LoopMode parseLoopMode(String value) {
    return switch (value.toLowerCase(Locale.ROOT)) {
      case "once", "" -> LoopMode.ONCE;
      case "loop" -> LoopMode.LOOP;
      case "pingpong", "ping_pong", "ping-pong" -> LoopMode.PING_PONG;
      default ->
          throw new IllegalArgumentException(
              "Unsupported Blockbench animation loop mode: " + value);
    };
  }

  static Easing parseEasing(String value) {
    return switch (value.toLowerCase(Locale.ROOT)) {
      case "linear" -> Easing.LINEAR;
      case "step" -> Easing.STEP;
      case "catmullrom" -> Easing.EASE_IN_OUT_CUBIC;
      default ->
          throw new IllegalArgumentException("Unsupported Blockbench interpolation: " + value);
    };
  }

  static String decodeUtf8(byte[] bytes, String path) {
    try {
      return StandardCharsets.UTF_8
          .newDecoder()
          .onMalformedInput(CodingErrorAction.REPORT)
          .onUnmappableCharacter(CodingErrorAction.REPORT)
          .decode(java.nio.ByteBuffer.wrap(bytes))
          .toString();
    } catch (CharacterCodingException exception) {
      throw new IllegalArgumentException("Bundle entry is not valid UTF-8: " + path, exception);
    }
  }

  static String sha256(byte[] bytes) {
    try {
      byte[] digest = MessageDigest.getInstance("SHA-256").digest(bytes);
      StringBuilder result = new StringBuilder(digest.length * 2);
      for (byte value : digest) {
        result.append(String.format(Locale.ROOT, "%02x", value));
      }
      return result.toString();
    } catch (NoSuchAlgorithmException exception) {
      throw new IllegalStateException("SHA-256 is required by the Java runtime", exception);
    }
  }
}
