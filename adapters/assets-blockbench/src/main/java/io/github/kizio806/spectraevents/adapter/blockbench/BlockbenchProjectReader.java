package io.github.kizio806.spectraevents.adapter.blockbench;

import static io.github.kizio806.spectraevents.adapter.blockbench.BlockbenchJsonSupport.MAX_JSON_DEPTH;
import static io.github.kizio806.spectraevents.adapter.blockbench.BlockbenchJsonSupport.assertJsonDepth;
import static io.github.kizio806.spectraevents.adapter.blockbench.BlockbenchJsonSupport.dataPointVector;
import static io.github.kizio806.spectraevents.adapter.blockbench.BlockbenchJsonSupport.decodeUtf8;
import static io.github.kizio806.spectraevents.adapter.blockbench.BlockbenchJsonSupport.optionalFiniteFloat;
import static io.github.kizio806.spectraevents.adapter.blockbench.BlockbenchJsonSupport.optionalInt;
import static io.github.kizio806.spectraevents.adapter.blockbench.BlockbenchJsonSupport.optionalString;
import static io.github.kizio806.spectraevents.adapter.blockbench.BlockbenchJsonSupport.optionalVector;
import static io.github.kizio806.spectraevents.adapter.blockbench.BlockbenchJsonSupport.parseEasing;
import static io.github.kizio806.spectraevents.adapter.blockbench.BlockbenchJsonSupport.parseLoopMode;
import static io.github.kizio806.spectraevents.adapter.blockbench.BlockbenchJsonSupport.parseObject;
import static io.github.kizio806.spectraevents.adapter.blockbench.BlockbenchJsonSupport.requireObject;
import static io.github.kizio806.spectraevents.adapter.blockbench.BlockbenchJsonSupport.requiredArray;
import static io.github.kizio806.spectraevents.adapter.blockbench.BlockbenchJsonSupport.requiredFiniteDouble;
import static io.github.kizio806.spectraevents.adapter.blockbench.BlockbenchJsonSupport.requiredInt;
import static io.github.kizio806.spectraevents.adapter.blockbench.BlockbenchJsonSupport.requiredObject;
import static io.github.kizio806.spectraevents.adapter.blockbench.BlockbenchJsonSupport.requiredString;
import static io.github.kizio806.spectraevents.adapter.blockbench.BlockbenchJsonSupport.requiredUv;
import static io.github.kizio806.spectraevents.adapter.blockbench.BlockbenchJsonSupport.requiredVector;
import static io.github.kizio806.spectraevents.adapter.blockbench.BlockbenchJsonSupport.sha256;
import static io.github.kizio806.spectraevents.adapter.blockbench.BlockbenchJsonSupport.toEuler;
import static io.github.kizio806.spectraevents.adapter.blockbench.SafeArchiveReader.MAX_ENTRY_BYTES;
import static io.github.kizio806.spectraevents.adapter.blockbench.SafeArchiveReader.readArchive;
import static io.github.kizio806.spectraevents.adapter.blockbench.SafeArchiveReader.requiredEntry;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.github.kizio806.spectraevents.application.port.AssetImportPort;
import io.github.kizio806.spectraevents.core.visual.animation.AnimationDuration;
import io.github.kizio806.spectraevents.core.visual.animation.AnimationTime;
import io.github.kizio806.spectraevents.core.visual.animation.Easing;
import io.github.kizio806.spectraevents.core.visual.animation.LoopMode;
import io.github.kizio806.spectraevents.core.visual.animation.RotationKeyframe;
import io.github.kizio806.spectraevents.core.visual.animation.RotationMode;
import io.github.kizio806.spectraevents.core.visual.animation.ScaleKeyframe;
import io.github.kizio806.spectraevents.core.visual.animation.Vector3Keyframe;
import io.github.kizio806.spectraevents.core.visual.asset.SpectraAssetAnimation;
import io.github.kizio806.spectraevents.core.visual.asset.SpectraAssetDocument;
import io.github.kizio806.spectraevents.core.visual.asset.SpectraAssetFace;
import io.github.kizio806.spectraevents.core.visual.asset.SpectraAssetGeometry;
import io.github.kizio806.spectraevents.core.visual.asset.SpectraAssetNode;
import io.github.kizio806.spectraevents.core.visual.asset.SpectraAssetTexture;
import io.github.kizio806.spectraevents.core.visual.model.EulerRotation;
import io.github.kizio806.spectraevents.core.visual.model.Vector3;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;

/** Imports a validated Blockbench project or signed Spectra bundle into the asset model. */
public final class BlockbenchProjectReader implements AssetImportPort {
  private static final long MAX_ARCHIVE_BYTES = 25_000_000L;
  private static final int MAX_TEXTURES = 16;
  private static final int MAX_NODES = 128;
  private static final int MAX_CUBES = 512;
  private static final int MAX_ANIMATIONS = 32;
  private static final int MAX_KEYFRAMES = 2_000;
  private static final int MAX_TEXTURE_DIMENSION = 4096;
  private static final long MAX_TEXTURE_PIXELS = 16_777_216L;
  private static final String MANIFEST_FILE = "manifest.json";
  private static final String MODEL_FILE = "model.bbmodel";
  private static final Set<String> SUPPORTED_MODEL_FORMATS = Set.of("free", "java_block");
  private static final Pattern MODEL_ID = Pattern.compile("[a-z0-9][a-z0-9_-]{0,63}");
  private static final Pattern TEXTURE_PATH =
      Pattern.compile("textures/[a-z0-9][a-z0-9._-]{0,63}\\.png");
  private static final Set<String> FACE_NAMES =
      Set.of("north", "south", "east", "west", "up", "down");

  @Override
  public SpectraAssetDocument read(Path sourceFile) throws IOException {
    Objects.requireNonNull(sourceFile, "sourceFile");
    if (!Files.isRegularFile(sourceFile)) {
      throw new IllegalArgumentException("Asset source must be a regular file");
    }
    Path namePath = sourceFile.getFileName();
    if (namePath == null) {
      throw new IllegalArgumentException("Asset source has no filename");
    }
    String fileName = namePath.toString();
    if (fileName.endsWith(".bbmodel")) {
      if (Files.size(sourceFile) > MAX_ENTRY_BYTES) {
        throw new IllegalArgumentException(
            "Blockbench project exceeds size limit of " + MAX_ENTRY_BYTES + " bytes");
      }
      String modelId = fileName.substring(0, fileName.length() - ".bbmodel".length());
      if (!MODEL_ID.matcher(modelId).matches()) {
        throw new IllegalArgumentException("Blockbench filename must match " + MODEL_ID.pattern());
      }
      return parseProject(Files.readString(sourceFile, StandardCharsets.UTF_8), modelId, Map.of());
    }
    if (!fileName.endsWith(".spectra.zip")) {
      throw new IllegalArgumentException(
          "Supported asset source must be a .bbmodel or .spectra.zip file");
    }
    if (Files.size(sourceFile) > MAX_ARCHIVE_BYTES) {
      throw new IllegalArgumentException(
          "Asset bundle exceeds compressed size limit of " + MAX_ARCHIVE_BYTES + " bytes");
    }

    Map<String, byte[]> entries = readArchive(sourceFile);
    BundleManifest manifest = parseManifest(requiredEntry(entries, MANIFEST_FILE));
    validateBundleLayout(entries, manifest);

    Map<String, byte[]> textures = new HashMap<>();
    for (String texturePath : manifest.texturePaths()) {
      textures.put(texturePath, requiredEntry(entries, texturePath));
    }
    return parseProject(
        decodeUtf8(requiredEntry(entries, MODEL_FILE), MODEL_FILE), manifest.modelId(), textures);
  }

  private BundleManifest parseManifest(byte[] manifestBytes) {
    String manifestJson = decodeUtf8(manifestBytes, MANIFEST_FILE);
    assertJsonDepth(manifestJson, MAX_JSON_DEPTH);
    JsonObject manifest = parseObject(manifestJson, MANIFEST_FILE);
    int schemaVersion = requiredInt(manifest, "schemaVersion", MANIFEST_FILE);
    if (schemaVersion != 1) {
      throw new IllegalArgumentException(
          "Unsupported Spectra bundle schemaVersion: " + schemaVersion);
    }
    String modelId = requiredString(manifest, "modelId", MANIFEST_FILE);
    if (!MODEL_ID.matcher(modelId).matches()) {
      throw new IllegalArgumentException("manifest.json modelId must match " + MODEL_ID.pattern());
    }
    if (!MODEL_FILE.equals(requiredString(manifest, "model", MANIFEST_FILE))) {
      throw new IllegalArgumentException("manifest.json model must be exactly model.bbmodel");
    }

    JsonArray textureArray = requiredArray(manifest, "textures", MANIFEST_FILE);
    if (textureArray.size() > MAX_TEXTURES) {
      throw new IllegalArgumentException("manifest.json contains more than 16 textures");
    }
    Set<String> textures = new LinkedHashSet<>();
    for (JsonElement textureElement : textureArray) {
      if (!textureElement.isJsonPrimitive() || !textureElement.getAsJsonPrimitive().isString()) {
        throw new IllegalArgumentException("manifest.json textures must contain only strings");
      }
      String texturePath = textureElement.getAsString();
      if (!TEXTURE_PATH.matcher(texturePath).matches() || !textures.add(texturePath)) {
        throw new IllegalArgumentException(
            "manifest.json has invalid or duplicate texture path: " + texturePath);
      }
    }

    JsonObject checksumsObject = requiredObject(manifest, "sha256", MANIFEST_FILE);
    Map<String, String> checksums = new HashMap<>();
    for (Map.Entry<String, JsonElement> entry : checksumsObject.entrySet()) {
      String path = entry.getKey();
      if (!entry.getValue().isJsonPrimitive()
          || !entry.getValue().getAsJsonPrimitive().isString()) {
        throw new IllegalArgumentException("manifest.json sha256 values must be strings");
      }
      String checksum = entry.getValue().getAsString().toLowerCase(Locale.ROOT);
      if (!checksum.matches("[0-9a-f]{64}")) {
        throw new IllegalArgumentException("manifest.json has invalid SHA-256 for " + path);
      }
      checksums.put(path, checksum);
    }
    return new BundleManifest(modelId, Set.copyOf(textures), Map.copyOf(checksums));
  }

  private void validateBundleLayout(Map<String, byte[]> entries, BundleManifest manifest) {
    Set<String> expectedFiles = new HashSet<>();
    expectedFiles.add(MANIFEST_FILE);
    expectedFiles.add(MODEL_FILE);
    expectedFiles.addAll(manifest.texturePaths());
    if (!entries.keySet().equals(expectedFiles)) {
      throw new IllegalArgumentException(
          "Asset bundle contains files outside the declared manifest");
    }

    Set<String> checksumFiles = new HashSet<>();
    checksumFiles.add(MODEL_FILE);
    checksumFiles.addAll(manifest.texturePaths());
    if (!manifest.checksums().keySet().equals(checksumFiles)) {
      throw new IllegalArgumentException(
          "manifest.json sha256 must cover exactly model.bbmodel and textures");
    }
    for (String path : checksumFiles) {
      if (!sha256(requiredEntry(entries, path)).equals(manifest.checksums().get(path))) {
        throw new IllegalArgumentException("SHA-256 mismatch for bundle entry " + path);
      }
    }
  }

  private SpectraAssetDocument parseProject(
      String jsonContent, String modelId, Map<String, byte[]> bundleTextures) {
    assertJsonDepth(jsonContent, MAX_JSON_DEPTH);
    JsonObject root = parseObject(jsonContent, MODEL_FILE);
    JsonObject meta = requiredObject(root, "meta", MODEL_FILE);
    String formatVersion = requiredString(meta, "format_version", MODEL_FILE);
    if (!formatVersion.startsWith("4.") && !formatVersion.startsWith("5.")) {
      throw new IllegalArgumentException(
          "Unsupported Blockbench format version: "
              + formatVersion
              + ". Only 4.x and 5.x are supported.");
    }
    String modelFormat = requiredString(meta, "model_format", MODEL_FILE);
    if (!SUPPORTED_MODEL_FORMATS.contains(modelFormat)) {
      throw new IllegalArgumentException(
          "Only Blockbench Generic Model (model_format=free) and Java Block "
              + "(model_format=java_block) projects are supported");
    }

    Map<String, SpectraAssetTexture> textures =
        parseTextures(requiredArray(root, "textures", MODEL_FILE), bundleTextures);
    Map<String, SpectraAssetGeometry> elements =
        parseElements(
            requiredArray(root, "elements", MODEL_FILE),
            textures.keySet(),
            List.copyOf(textures.keySet()));
    ParsedOutliner outliner = parseOutliner(requiredArray(root, "outliner", MODEL_FILE), elements);
    List<SpectraAssetNode> nodes = outliner.nodes();
    if (nodes.isEmpty()) {
      throw new IllegalArgumentException("Blockbench project must contain at least one root group");
    }
    Map<String, SpectraAssetAnimation> animations =
        parseAnimations(root, outliner.nodeIdsByBlockbenchUuid());
    return new SpectraAssetDocument(1, modelId, textures, nodes, animations);
  }

  private Map<String, SpectraAssetTexture> parseTextures(
      JsonArray textureArray, Map<String, byte[]> bundleTextures) {
    if (textureArray.size() > MAX_TEXTURES) {
      throw new IllegalArgumentException("Blockbench project contains more than 16 textures");
    }
    Map<String, SpectraAssetTexture> textures = new LinkedHashMap<>();
    for (JsonElement textureElement : textureArray) {
      JsonObject texture = requireObject(textureElement, "textures[]");
      String textureId = requiredString(texture, "id", "textures[]");
      String textureName = requiredString(texture, "name", "textures[]");
      String source = requiredString(texture, "source", "textures[]");
      byte[] data;
      if (source.startsWith("data:image/png;base64,")
          || source.startsWith("data:image/jpeg;base64,")) {
        data = decodeEmbeddedTexture(source);
      } else {
        if (!TEXTURE_PATH.matcher(source).matches()) {
          throw new IllegalArgumentException(
              "Texture source must be a declared textures/*.png bundle path");
        }
        data = bundleTextures.get(source);
        if (data == null) {
          throw new IllegalArgumentException(
              "Texture source is not present in the bundle: " + source);
        }
      }
      data = normalizeTextureToPng(data, textureName);
      if (textures.putIfAbsent(textureId, new SpectraAssetTexture(textureName, data, null))
          != null) {
        throw new IllegalArgumentException(
            "Blockbench project contains duplicate texture id: " + textureId);
      }
    }
    return Collections.unmodifiableMap(new LinkedHashMap<>(textures));
  }

  private Map<String, SpectraAssetGeometry> parseElements(
      JsonArray elementArray, Set<String> textureIds, List<String> textureIdsByIndex) {
    if (elementArray.size() > MAX_CUBES) {
      throw new IllegalArgumentException("Blockbench project contains more than 512 cubes");
    }
    Map<String, SpectraAssetGeometry> elements = new LinkedHashMap<>();
    for (JsonElement elementValue : elementArray) {
      JsonObject element = requireObject(elementValue, "elements[]");
      String id = requiredString(element, "uuid", "elements[]");
      if (!"cube".equals(optionalString(element, "type", "cube"))) {
        throw new IllegalArgumentException(
            "elements."
                + id
                + " uses unsupported Blockbench mesh geometry; export cuboid elements for the "
                + "Minecraft resource-pack generator");
      }
      Vector3 from = requiredVector(element, "from", "elements." + id);
      Vector3 to = requiredVector(element, "to", "elements." + id);
      Vector3 origin = optionalVector(element, "origin", Vector3.ZERO, "elements." + id);
      EulerRotation rotation =
          toEuler(optionalVector(element, "rotation", Vector3.ZERO, "elements." + id));
      float inflate = optionalFiniteFloat(element, "inflate", 0.0f, "elements." + id);
      if (Float.compare(from.x(), to.x()) == 0
          || Float.compare(from.y(), to.y()) == 0
          || Float.compare(from.z(), to.z()) == 0) {
        throw new IllegalArgumentException("Cube " + id + " must have non-zero size on every axis");
      }
      Vector3 normalizedFrom =
          new Vector3(
              Math.min(from.x(), to.x()), Math.min(from.y(), to.y()), Math.min(from.z(), to.z()));
      Vector3 normalizedTo =
          new Vector3(
              Math.max(from.x(), to.x()), Math.max(from.y(), to.y()), Math.max(from.z(), to.z()));
      Map<String, SpectraAssetFace> faces =
          parseFaces(
              requiredObject(element, "faces", "elements." + id),
              textureIds,
              textureIdsByIndex,
              id);
      if (elements.putIfAbsent(
              id,
              new SpectraAssetGeometry(
                  normalizedFrom, normalizedTo, origin, rotation, inflate, faces))
          != null) {
        throw new IllegalArgumentException(
            "Blockbench project contains duplicate cube uuid: " + id);
      }
    }
    if (elements.isEmpty()) {
      throw new IllegalArgumentException("Blockbench project must contain at least one cube");
    }
    return Map.copyOf(elements);
  }

  private Map<String, SpectraAssetFace> parseFaces(
      JsonObject faceObject,
      Set<String> textureIds,
      List<String> textureIdsByIndex,
      String elementId) {
    Map<String, SpectraAssetFace> faces = new LinkedHashMap<>();
    for (Map.Entry<String, JsonElement> faceEntry : faceObject.entrySet()) {
      if (!FACE_NAMES.contains(faceEntry.getKey())) {
        throw new IllegalArgumentException(
            "Cube " + elementId + " has unsupported face " + faceEntry.getKey());
      }
      JsonObject face = requireObject(faceEntry.getValue(), "elements." + elementId + ".faces");
      List<Double> uv = requiredUv(face, "elements." + elementId + ".faces." + faceEntry.getKey());
      String textureRef = faceTextureReference(face, elementId);
      String textureId =
          resolveTextureId(textureRef, face.get("texture"), textureIdsByIndex, elementId);
      if (!textureIds.contains(textureId)) {
        throw new IllegalArgumentException(
            "Cube " + elementId + " references unknown texture " + textureRef);
      }
      int rotation = optionalInt(face, "rotation", 0, "elements." + elementId + ".faces");
      if (rotation != 0 && rotation != 90 && rotation != 180 && rotation != 270) {
        throw new IllegalArgumentException("Face rotation must be 0, 90, 180, or 270");
      }
      faces.put(faceEntry.getKey(), new SpectraAssetFace(uv, textureId, rotation));
    }
    if (faces.isEmpty()) {
      throw new IllegalArgumentException(
          "Cube " + elementId + " must define at least one textured face");
    }
    return Map.copyOf(faces);
  }

  private static String faceTextureReference(JsonObject face, String elementId) {
    JsonElement texture = face.get("texture");
    if (texture == null || !texture.isJsonPrimitive()) {
      throw new IllegalArgumentException(
          "elements." + elementId + ".faces.texture must be a texture ID or index");
    }
    if (texture.getAsJsonPrimitive().isString()) {
      String textureReference = texture.getAsString();
      if (textureReference.isBlank()) {
        throw new IllegalArgumentException(
            "elements." + elementId + ".faces.texture must not be blank");
      }
      return textureReference;
    }
    if (texture.getAsJsonPrimitive().isNumber()) {
      return texture.getAsString();
    }
    throw new IllegalArgumentException(
        "elements." + elementId + ".faces.texture must be a texture ID or index");
  }

  private static String resolveTextureId(
      String textureReference,
      JsonElement textureValue,
      List<String> textureIdsByIndex,
      String elementId) {
    if (textureValue.getAsJsonPrimitive().isString()) {
      return textureReference.startsWith("#") ? textureReference.substring(1) : textureReference;
    }
    try {
      int textureIndex = Integer.parseInt(textureReference);
      if (textureIndex < 0 || textureIndex >= textureIdsByIndex.size()) {
        throw new IllegalArgumentException(
            "Cube " + elementId + " references unknown texture index " + textureReference);
      }
      return textureIdsByIndex.get(textureIndex);
    } catch (NumberFormatException exception) {
      throw new IllegalArgumentException(
          "Cube " + elementId + " references invalid texture index " + textureReference, exception);
    }
  }

  private ParsedOutliner parseOutliner(
      JsonArray outliner, Map<String, SpectraAssetGeometry> elements) {
    List<SpectraAssetNode> nodes = new ArrayList<>();
    Set<String> nodeIds = new HashSet<>();
    Map<String, String> nodeIdsByBlockbenchUuid = new HashMap<>();
    Set<String> referencedElements = new HashSet<>();
    for (JsonElement value : outliner) {
      nodes.add(
          parseNode(
              requireObject(value, "outliner[]"),
              elements,
              nodeIds,
              nodeIdsByBlockbenchUuid,
              referencedElements,
              1));
    }
    if (!referencedElements.equals(elements.keySet())) {
      throw new IllegalArgumentException(
          "Every Blockbench cube must belong to exactly one outliner group");
    }
    return new ParsedOutliner(List.copyOf(nodes), Map.copyOf(nodeIdsByBlockbenchUuid));
  }

  private SpectraAssetNode parseNode(
      JsonObject group,
      Map<String, SpectraAssetGeometry> elements,
      Set<String> nodeIds,
      Map<String, String> nodeIdsByBlockbenchUuid,
      Set<String> referencedElements,
      int depth) {
    if (depth > 32) {
      throw new IllegalArgumentException("Blockbench group hierarchy exceeds maximum depth of 32");
    }
    if (nodeIds.size() >= MAX_NODES) {
      throw new IllegalArgumentException("Blockbench project contains more than 128 groups");
    }
    String blockbenchUuid = requiredString(group, "uuid", "outliner group");
    String nodeId = normalizeNodeId(blockbenchUuid);
    if (!nodeIds.add(nodeId)
        || nodeIdsByBlockbenchUuid.putIfAbsent(blockbenchUuid, nodeId) != null) {
      throw new IllegalArgumentException(
          "Blockbench project contains duplicate or conflicting group uuid: " + blockbenchUuid);
    }
    Vector3 pivot = optionalVector(group, "origin", Vector3.ZERO, "outliner." + nodeId);
    EulerRotation rotation =
        toEuler(optionalVector(group, "rotation", Vector3.ZERO, "outliner." + nodeId));
    JsonArray children = requiredArray(group, "children", "outliner." + nodeId);
    List<SpectraAssetNode> childNodes = new ArrayList<>();
    List<SpectraAssetGeometry> cubes = new ArrayList<>();
    for (JsonElement child : children) {
      if (child.isJsonPrimitive() && child.getAsJsonPrimitive().isString()) {
        String cubeId = child.getAsString();
        SpectraAssetGeometry cube = elements.get(cubeId);
        if (cube == null || !referencedElements.add(cubeId)) {
          throw new IllegalArgumentException(
              "Outliner references an unknown or duplicate cube: " + cubeId);
        }
        cubes.add(cube);
      } else if (child.isJsonObject()) {
        childNodes.add(
            parseNode(
                child.getAsJsonObject(),
                elements,
                nodeIds,
                nodeIdsByBlockbenchUuid,
                referencedElements,
                depth + 1));
      } else {
        throw new IllegalArgumentException(
            "Outliner children must be group objects or cube UUID strings");
      }
    }
    return new SpectraAssetNode(
        nodeId, pivot, Vector3.ZERO, rotation, Vector3.ONE, cubes, childNodes);
  }

  private Map<String, SpectraAssetAnimation> parseAnimations(
      JsonObject root, Map<String, String> nodeIdsByBlockbenchUuid) {
    JsonArray animationArray =
        root.has("animations") ? requiredArray(root, "animations", MODEL_FILE) : new JsonArray();
    if (animationArray.size() > MAX_ANIMATIONS) {
      throw new IllegalArgumentException("Blockbench project contains more than 32 animations");
    }
    Map<String, SpectraAssetAnimation> animations = new LinkedHashMap<>();
    int totalKeyframes = 0;
    for (JsonElement animationValue : animationArray) {
      JsonObject animation = requireObject(animationValue, "animations[]");
      String name = requiredString(animation, "name", "animations[]");
      double length = requiredFiniteDouble(animation, "length", "animations." + name);
      if (length <= 0.0 || length > 3600.0) {
        throw new IllegalArgumentException(
            "Animation " + name + " length must be between 0 and 3600 seconds");
      }
      LoopMode loopMode = parseLoopMode(optionalString(animation, "loop", "once"));
      Map<String, List<Vector3Keyframe>> translations = new HashMap<>();
      Map<String, List<RotationKeyframe>> rotations = new HashMap<>();
      Map<String, List<ScaleKeyframe>> scales = new HashMap<>();
      JsonObject animators =
          animation.has("animators")
              ? requiredObject(animation, "animators", "animations." + name)
              : new JsonObject();
      for (Map.Entry<String, JsonElement> animatorEntry : animators.entrySet()) {
        String blockbenchUuid = animatorEntry.getKey();
        String nodeId = nodeIdsByBlockbenchUuid.get(blockbenchUuid);
        if (nodeId == null) {
          throw new IllegalArgumentException(
              "Animation " + name + " targets unknown group " + blockbenchUuid);
        }
        JsonObject animator =
            requireObject(animatorEntry.getValue(), "animations." + name + ".animators");
        JsonArray keyframes =
            animator.has("keyframes")
                ? requiredArray(animator, "keyframes", "animator")
                : new JsonArray();
        for (JsonElement keyframeValue : keyframes) {
          totalKeyframes++;
          if (totalKeyframes > MAX_KEYFRAMES) {
            throw new IllegalArgumentException(
                "Blockbench project contains more than 2000 animation keyframes");
          }
          JsonObject keyframe = requireObject(keyframeValue, "animation keyframe");
          AnimationTime time =
              AnimationTime.fromSeconds(
                  requiredFiniteDouble(keyframe, "time", "animation keyframe"));
          if (time.toSeconds() > length) {
            throw new IllegalArgumentException(
                "Animation keyframe exceeds animation length for " + name);
          }
          Easing easing = parseEasing(optionalString(keyframe, "interpolation", "linear"));
          Vector3 value = dataPointVector(keyframe, "animation keyframe");
          switch (requiredString(keyframe, "channel", "animation keyframe")) {
            case "position" ->
                translations
                    .computeIfAbsent(nodeId, ignored -> new ArrayList<>())
                    .add(new Vector3Keyframe(time, value, easing));
            case "rotation" ->
                rotations
                    .computeIfAbsent(nodeId, ignored -> new ArrayList<>())
                    .add(
                        RotationKeyframe.ofEuler(
                            time, toEuler(value), easing, RotationMode.CONTINUOUS));
            case "scale" ->
                scales
                    .computeIfAbsent(nodeId, ignored -> new ArrayList<>())
                    .add(new ScaleKeyframe(time, value, easing));
            default ->
                throw new IllegalArgumentException("Unsupported Blockbench animation channel");
          }
        }
      }
      sortTracks(translations, rotations, scales);
      SpectraAssetAnimation imported =
          new SpectraAssetAnimation(
              name,
              AnimationDuration.fromSeconds(length),
              loopMode,
              translations,
              rotations,
              scales,
              List.of());
      if (animations.putIfAbsent(name, imported) != null) {
        throw new IllegalArgumentException(
            "Blockbench project contains duplicate animation name: " + name);
      }
    }
    return Map.copyOf(animations);
  }

  private static void sortTracks(
      Map<String, List<Vector3Keyframe>> translations,
      Map<String, List<RotationKeyframe>> rotations,
      Map<String, List<ScaleKeyframe>> scales) {
    translations.values().forEach(keyframes -> keyframes.sort(null));
    rotations.values().forEach(keyframes -> keyframes.sort(null));
    scales.values().forEach(keyframes -> keyframes.sort(null));
  }

  private static String normalizeNodeId(String blockbenchUuid) {
    String nodeId = blockbenchUuid.replace("-", "");
    if (!MODEL_ID.matcher(nodeId).matches()) {
      throw new IllegalArgumentException(
          "Blockbench group uuid must normalize to " + MODEL_ID.pattern());
    }
    return nodeId;
  }

  private static byte[] decodeEmbeddedTexture(String source) {
    try {
      return Base64.getDecoder().decode(source.substring(source.indexOf(',') + 1));
    } catch (IllegalArgumentException exception) {
      throw new IllegalArgumentException(
          "Embedded texture is not valid Base64 PNG data", exception);
    }
  }

  private static byte[] normalizeTextureToPng(byte[] data, String textureName) {
    try (ImageInputStream input = ImageIO.createImageInputStream(new ByteArrayInputStream(data))) {
      Iterator<ImageReader> readers = ImageIO.getImageReaders(input);
      if (!readers.hasNext()) {
        throw new IllegalArgumentException("Texture is not a decodable image: " + textureName);
      }
      ImageReader reader = readers.next();
      try {
        reader.setInput(input, true, true);
        int width = reader.getWidth(0);
        int height = reader.getHeight(0);
        if (width <= 0
            || height <= 0
            || width > MAX_TEXTURE_DIMENSION
            || height > MAX_TEXTURE_DIMENSION
            || (long) width * height > MAX_TEXTURE_PIXELS) {
          throw new IllegalArgumentException(
              "Texture dimensions exceed 4096x4096 limit: " + textureName);
        }
        if ("png".equalsIgnoreCase(reader.getFormatName())) {
          return data;
        }
        if (!"jpeg".equalsIgnoreCase(reader.getFormatName())
            && !"jpg".equalsIgnoreCase(reader.getFormatName())) {
          throw new IllegalArgumentException("Texture must be PNG or JPEG: " + textureName);
        }
        BufferedImage image = reader.read(0);
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        if (!ImageIO.write(image, "png", output)) {
          throw new IllegalArgumentException("Could not encode texture as PNG: " + textureName);
        }
        return output.toByteArray();
      } finally {
        reader.dispose();
      }
    } catch (IOException exception) {
      throw new IllegalArgumentException("Could not inspect texture " + textureName, exception);
    }
  }

  private record ParsedOutliner(
      List<SpectraAssetNode> nodes, Map<String, String> nodeIdsByBlockbenchUuid) {}

  private record BundleManifest(
      String modelId, Set<String> texturePaths, Map<String, String> checksums) {}
}
