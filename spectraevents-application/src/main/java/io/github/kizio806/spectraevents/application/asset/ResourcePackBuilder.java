package io.github.kizio806.spectraevents.application.asset;

import io.github.kizio806.spectraevents.core.visual.asset.SpectraAssetDocument;
import io.github.kizio806.spectraevents.core.visual.asset.SpectraAssetFace;
import io.github.kizio806.spectraevents.core.visual.asset.SpectraAssetGeometry;
import io.github.kizio806.spectraevents.core.visual.asset.SpectraAssetNode;
import io.github.kizio806.spectraevents.core.visual.asset.SpectraAssetTexture;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/** Builds a deterministic local Minecraft resource pack from verified Spectra asset documents. */
public final class ResourcePackBuilder {
  private static final String NAMESPACE = GeneratedAssetItem.NAMESPACE;
  private static final String BASE_ITEM = "paper";

  private final Path outputDirectory;

  public ResourcePackBuilder(Path outputDirectory) {
    this.outputDirectory =
        Objects.requireNonNull(outputDirectory, "outputDirectory").toAbsolutePath().normalize();
  }

  public ResourcePackBuildResult build(
      Collection<SpectraAssetDocument> documents, AssetTargetProfile profile) throws IOException {
    Objects.requireNonNull(documents, "documents");
    Objects.requireNonNull(profile, "profile");
    if (documents.isEmpty()) {
      throw new IllegalArgumentException(
          "Cannot build a resource pack without imported asset documents");
    }

    List<SpectraAssetDocument> sortedDocuments =
        documents.stream().sorted(Comparator.comparing(SpectraAssetDocument::modelId)).toList();
    ensureUniqueModelIds(sortedDocuments);
    List<ModelNode> modelNodes = collectNodes(sortedDocuments);
    Map<String, Integer> customModelData = allocateCustomModelData(modelNodes);
    Map<String, byte[]> entries =
        buildEntries(sortedDocuments, modelNodes, customModelData, profile);

    Files.createDirectories(outputDirectory);
    Path target =
        outputDirectory.resolve(
            "spectraevents-" + profile.name().toLowerCase(Locale.ROOT) + ".zip");
    Path temporary = Files.createTempFile(outputDirectory, "spectraevents-", ".zip.tmp");
    try {
      writeZip(temporary, entries);
      moveAtomically(temporary, target);
    } finally {
      Files.deleteIfExists(temporary);
    }

    return new ResourcePackBuildResult(
        target,
        profile,
        digest(target, "SHA-1"),
        digest(target, "SHA-256"),
        Files.size(target),
        customModelData);
  }

  private Map<String, byte[]> buildEntries(
      List<SpectraAssetDocument> documents,
      List<ModelNode> modelNodes,
      Map<String, Integer> customModelData,
      AssetTargetProfile profile) {
    Map<String, byte[]> entries = new TreeMap<>();
    entries.put("pack.mcmeta", packMeta(profile).getBytes(StandardCharsets.UTF_8));
    entries.put(
        "assets/minecraft/items/" + BASE_ITEM + ".json",
        baseItemOverrides(modelNodes, customModelData).getBytes(StandardCharsets.UTF_8));

    for (SpectraAssetDocument document : documents) {
      Map<String, String> textureLocations = writeTextures(entries, document);
      for (ModelNode node : modelNodes) {
        if (!node.document().equals(document)) {
          continue;
        }
        String path =
            "assets/"
                + NAMESPACE
                + "/models/item/"
                + document.modelId()
                + "/"
                + node.node().nodeId()
                + ".json";
        entries.put(
            path, modelJson(node.node(), textureLocations).getBytes(StandardCharsets.UTF_8));
      }
    }
    entries.put(
        "assets/" + NAMESPACE + "/spectraevents-manifest.json",
        manifestJson(entries, customModelData, profile).getBytes(StandardCharsets.UTF_8));
    return Map.copyOf(entries);
  }

  private static Map<String, String> writeTextures(
      Map<String, byte[]> entries, SpectraAssetDocument document) {
    Map<String, String> locations = new HashMap<>();
    int index = 0;
    for (Map.Entry<String, SpectraAssetTexture> texture :
        document.textures().entrySet().stream().sorted(Map.Entry.comparingByKey()).toList()) {
      String textureName = "texture_" + index++;
      byte[] data =
          texture
              .getValue()
              .data()
              .orElseThrow(
                  () ->
                      new IllegalArgumentException(
                          "Imported texture must contain embedded bytes: " + texture.getKey()));
      String path =
          "assets/"
              + NAMESPACE
              + "/textures/item/"
              + document.modelId()
              + "/"
              + textureName
              + ".png";
      entries.put(path, data);
      locations.put(
          texture.getKey(), NAMESPACE + ":item/" + document.modelId() + "/" + textureName);
    }
    return Map.copyOf(locations);
  }

  private static String packMeta(AssetTargetProfile profile) {
    return String.format(
        Locale.ROOT,
        "{\"pack\":{\"description\":\"SpectraEvents generated assets\",\"min_format\":[%d,%d],\"max_format\":[%d,%d]}}",
        profile.getMajorFormat(),
        profile.getMinorFormat(),
        profile.getMajorFormat(),
        profile.getMinorFormat());
  }

  private static String baseItemOverrides(
      List<ModelNode> modelNodes, Map<String, Integer> customModelData) {
    List<ModelNode> orderedNodes =
        modelNodes.stream()
            .sorted(Comparator.comparingInt(node -> customModelData.get(node.key())))
            .toList();
    StringBuilder json =
        new StringBuilder(
            "{\"model\":{\"type\":\"minecraft:range_dispatch\",\"property\":\"minecraft:custom_model_data\",\"entries\":[");
    for (int index = 0; index < orderedNodes.size(); index++) {
      if (index > 0) {
        json.append(',');
      }
      ModelNode node = orderedNodes.get(index);
      json.append("{\"threshold\":")
          .append(customModelData.get(node.key()))
          .append(",\"model\":{\"type\":\"minecraft:model\",\"model\":\"")
          .append(NAMESPACE)
          .append(":item/")
          .append(node.document().modelId())
          .append('/')
          .append(node.node().nodeId())
          .append("\"}}");
    }
    return json.append(
            "],\"fallback\":{\"type\":\"minecraft:model\",\"model\":\"minecraft:item/paper\"}}}")
        .toString();
  }

  private static String modelJson(SpectraAssetNode node, Map<String, String> textureLocations) {
    StringBuilder json = new StringBuilder("{\"textures\":{");
    List<String> textureIds = textureLocations.keySet().stream().sorted().toList();
    for (int index = 0; index < textureIds.size(); index++) {
      if (index > 0) {
        json.append(',');
      }
      String textureId = textureIds.get(index);
      json.append(jsonString(textureId))
          .append(':')
          .append(jsonString(textureLocations.get(textureId)));
    }
    json.append("},\"elements\":[");
    for (int index = 0; index < node.cubes().size(); index++) {
      if (index > 0) {
        json.append(',');
      }
      appendGeometry(json, node.cubes().get(index));
    }
    return json.append("]}").toString();
  }

  private static void appendGeometry(StringBuilder json, SpectraAssetGeometry geometry) {
    json.append("{\"from\":")
        .append(vector(geometry.from().x(), geometry.from().y(), geometry.from().z()))
        .append(",\"to\":")
        .append(vector(geometry.to().x(), geometry.to().y(), geometry.to().z()));
    if (!isZero(geometry.rotation().pitchX())
        || !isZero(geometry.rotation().yawY())
        || !isZero(geometry.rotation().rollZ())) {
      json.append(",\"rotation\":{\"origin\":")
          .append(vector(geometry.origin().x(), geometry.origin().y(), geometry.origin().z()))
          .append(",\"axis\":\"")
          .append(rotationAxis(geometry))
          .append("\",\"angle\":")
          .append(formatNumber(rotationAngle(geometry)))
          .append('}');
    }
    json.append(",\"faces\":{");
    List<String> faceNames = geometry.faces().keySet().stream().sorted().toList();
    for (int index = 0; index < faceNames.size(); index++) {
      if (index > 0) {
        json.append(',');
      }
      String faceName = faceNames.get(index);
      SpectraAssetFace face = geometry.faces().get(faceName);
      json.append(jsonString(faceName))
          .append(":{\"uv\":[")
          .append(formatNumber(face.uv().get(0)))
          .append(',')
          .append(formatNumber(face.uv().get(1)))
          .append(',')
          .append(formatNumber(face.uv().get(2)))
          .append(',')
          .append(formatNumber(face.uv().get(3)))
          .append("],\"texture\":\"#")
          .append(escape(face.textureRef()))
          .append("\"");
      if (face.rotation() != 0) {
        json.append(",\"rotation\":").append(face.rotation());
      }
      json.append('}');
    }
    json.append("}}");
  }

  private static String manifestJson(
      Map<String, byte[]> entries,
      Map<String, Integer> customModelData,
      AssetTargetProfile profile) {
    StringBuilder json = new StringBuilder("{\"schemaVersion\":1,\"profile\":\"");
    json.append(profile.name()).append("\",\"sha256\":{");
    List<String> paths = entries.keySet().stream().sorted().toList();
    for (int index = 0; index < paths.size(); index++) {
      if (index > 0) {
        json.append(',');
      }
      String path = paths.get(index);
      json.append(jsonString(path))
          .append(':')
          .append(jsonString(digest(entries.get(path), "SHA-256")));
    }
    json.append("},\"customModelData\":{");
    List<String> modelKeys = customModelData.keySet().stream().sorted().toList();
    for (int index = 0; index < modelKeys.size(); index++) {
      if (index > 0) {
        json.append(',');
      }
      String modelKey = modelKeys.get(index);
      json.append(jsonString(modelKey)).append(':').append(customModelData.get(modelKey));
    }
    return json.append("}}").toString();
  }

  private static List<ModelNode> collectNodes(List<SpectraAssetDocument> documents) {
    List<ModelNode> nodes = new ArrayList<>();
    for (SpectraAssetDocument document : documents) {
      document.nodes().forEach(node -> collectNode(document, node, nodes));
    }
    nodes.sort(Comparator.comparing(ModelNode::key));
    return List.copyOf(nodes);
  }

  private static void collectNode(
      SpectraAssetDocument document, SpectraAssetNode node, List<ModelNode> nodes) {
    validatePathSegment(document.modelId(), "model id");
    validatePathSegment(node.nodeId(), "Blockbench group UUID");
    nodes.add(new ModelNode(document, node));
    node.children().forEach(child -> collectNode(document, child, nodes));
  }

  private static Map<String, Integer> allocateCustomModelData(List<ModelNode> modelNodes) {
    Map<String, Integer> mappings = new LinkedHashMap<>();
    java.util.Set<Integer> allocatedValues = new HashSet<>();
    for (ModelNode node : modelNodes) {
      int customModelData =
          GeneratedAssetItem.customModelData(node.document().modelId(), node.node().nodeId());
      if (!allocatedValues.add(customModelData)) {
        throw new IllegalArgumentException(
            "Generated custom_model_data collision for imported asset node " + node.key());
      }
      mappings.put(node.key(), customModelData);
    }
    return Map.copyOf(mappings);
  }

  private static void ensureUniqueModelIds(List<SpectraAssetDocument> documents) {
    long distinct = documents.stream().map(SpectraAssetDocument::modelId).distinct().count();
    if (distinct != documents.size()) {
      throw new IllegalArgumentException(
          "Cannot build a resource pack with duplicate asset model IDs");
    }
  }

  private static void validatePathSegment(String value, String label) {
    if (!value.matches("[a-z0-9][a-z0-9_-]{0,63}")) {
      throw new IllegalArgumentException(label + " must match [a-z0-9][a-z0-9_-]{0,63}: " + value);
    }
  }

  private static void writeZip(Path target, Map<String, byte[]> entries) throws IOException {
    try (ZipOutputStream zip = new ZipOutputStream(Files.newOutputStream(target))) {
      for (Map.Entry<String, byte[]> entry : entries.entrySet()) {
        ZipEntry zipEntry = new ZipEntry(entry.getKey());
        zipEntry.setTime(0L);
        zip.putNextEntry(zipEntry);
        zip.write(entry.getValue());
        zip.closeEntry();
      }
    }
  }

  private static void moveAtomically(Path temporary, Path target) throws IOException {
    try {
      Files.move(
          temporary, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
    } catch (AtomicMoveNotSupportedException exception) {
      Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING);
    }
  }

  private static String digest(Path path, String algorithm) throws IOException {
    try (InputStream input = Files.newInputStream(path)) {
      MessageDigest messageDigest = MessageDigest.getInstance(algorithm);
      byte[] buffer = new byte[8192];
      int read;
      while ((read = input.read(buffer)) != -1) {
        messageDigest.update(buffer, 0, read);
      }
      return toHex(messageDigest.digest());
    } catch (NoSuchAlgorithmException exception) {
      throw new IllegalStateException(algorithm + " is required by the Java runtime", exception);
    }
  }

  private static String digest(byte[] bytes, String algorithm) {
    try {
      return toHex(MessageDigest.getInstance(algorithm).digest(bytes));
    } catch (NoSuchAlgorithmException exception) {
      throw new IllegalStateException(algorithm + " is required by the Java runtime", exception);
    }
  }

  private static String toHex(byte[] digest) {
    StringBuilder result = new StringBuilder(digest.length * 2);
    for (byte value : digest) {
      result.append(String.format(Locale.ROOT, "%02x", value));
    }
    return result.toString();
  }

  private static String vector(float x, float y, float z) {
    return "[" + formatNumber(x) + "," + formatNumber(y) + "," + formatNumber(z) + "]";
  }

  private static String formatNumber(double value) {
    if (!Double.isFinite(value)) {
      throw new IllegalArgumentException("Resource-pack geometry must contain finite numbers");
    }
    if (Double.compare(value, Math.rint(value)) == 0) {
      return Long.toString((long) value);
    }
    return Double.toString(value);
  }

  private static boolean isZero(float value) {
    return Math.abs(value) < 0.00001f;
  }

  private static String rotationAxis(SpectraAssetGeometry geometry) {
    if (!isZero(geometry.rotation().pitchX())) {
      return "x";
    }
    if (!isZero(geometry.rotation().yawY())) {
      return "y";
    }
    return "z";
  }

  private static float rotationAngle(SpectraAssetGeometry geometry) {
    if (!isZero(geometry.rotation().pitchX())) {
      return geometry.rotation().pitchX();
    }
    if (!isZero(geometry.rotation().yawY())) {
      return geometry.rotation().yawY();
    }
    return geometry.rotation().rollZ();
  }

  private static String jsonString(String value) {
    return "\"" + escape(value) + "\"";
  }

  private static String escape(String value) {
    return value.replace("\\", "\\\\").replace("\"", "\\\"");
  }

  private record ModelNode(SpectraAssetDocument document, SpectraAssetNode node) {
    private String key() {
      return document.modelId() + "/" + node.nodeId();
    }
  }
}
