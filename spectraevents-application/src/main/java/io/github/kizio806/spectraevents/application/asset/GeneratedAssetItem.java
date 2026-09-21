package io.github.kizio806.spectraevents.application.asset;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Objects;

/** Stable item reference and custom-model-data assignment for generated Spectra pack models. */
public final class GeneratedAssetItem {
  public static final String NAMESPACE = "spectraevents";
  public static final String BASE_MATERIAL = "minecraft:paper";
  private static final int FIRST_CUSTOM_MODEL_DATA = 100_000;
  private static final int CUSTOM_MODEL_DATA_RANGE = 9_000_000;

  private GeneratedAssetItem() {}

  public static String reference(String modelId, String nodeId) {
    validateSegment(modelId, "modelId");
    validateSegment(nodeId, "nodeId");
    return NAMESPACE + ":" + modelId + "/" + nodeId;
  }

  public static int customModelData(String modelId, String nodeId) {
    String reference = reference(modelId, nodeId);
    byte[] digest = sha256(reference);
    long value =
        ((long) (digest[0] & 0xff) << 24)
            | ((long) (digest[1] & 0xff) << 16)
            | ((long) (digest[2] & 0xff) << 8)
            | (digest[3] & 0xffL);
    return FIRST_CUSTOM_MODEL_DATA + (int) (value % CUSTOM_MODEL_DATA_RANGE);
  }

  public static ParsedReference parse(String reference) {
    Objects.requireNonNull(reference, "reference");
    String prefix = NAMESPACE + ":";
    if (!reference.startsWith(prefix)) {
      return null;
    }
    String path = reference.substring(prefix.length());
    String[] parts = path.split("/", -1);
    if (parts.length != 2) {
      throw new IllegalArgumentException("Generated asset item reference must be modelId/nodeId");
    }
    validateSegment(parts[0], "modelId");
    validateSegment(parts[1], "nodeId");
    return new ParsedReference(parts[0], parts[1]);
  }

  private static byte[] sha256(String value) {
    try {
      return MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
    } catch (NoSuchAlgorithmException exception) {
      throw new IllegalStateException("SHA-256 is required by the Java runtime", exception);
    }
  }

  private static void validateSegment(String value, String label) {
    if (value == null || !value.matches("[a-z0-9][a-z0-9_-]{0,63}")) {
      throw new IllegalArgumentException(label + " must match [a-z0-9][a-z0-9_-]{0,63}");
    }
  }

  public record ParsedReference(String modelId, String nodeId) {
    public int customModelData() {
      return GeneratedAssetItem.customModelData(modelId, nodeId);
    }
  }
}
