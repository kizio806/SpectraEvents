package io.github.kizio806.spectraevents.adapter.blockbench;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
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
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Base64;
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
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;

/** Imports the supported signed Spectra Blockbench bundle into the platform-neutral asset model. */
public final class BlockbenchProjectReader implements AssetImportPort {
  private static final long MAX_ARCHIVE_BYTES = 5_000_000L;
  private static final int MAX_ARCHIVE_ENTRIES = 18;
  private static final int MAX_ENTRY_BYTES = 2_000_000;
  private static final int MAX_TOTAL_UNCOMPRESSED_BYTES = 8_000_000;
  private static final int MAX_JSON_DEPTH = 64;
  private static final int MAX_TEXTURES = 16;
  private static final int MAX_NODES = 128;
  private static final int MAX_CUBES = 512;
  private static final int MAX_ANIMATIONS = 32;
  private static final int MAX_KEYFRAMES = 2_000;
  private static final int MAX_TEXTURE_DIMENSION = 1024;
  private static final long MAX_TEXTURE_PIXELS = 1_048_576L;
  private static final String MANIFEST_FILE = "manifest.json";
  private static final String MODEL_FILE = "model.bbmodel";
  private static final Pattern MODEL_ID = Pattern.compile("[a-z0-9][a-z0-9_-]{0,63}");
  private static final Pattern TEXTURE_PATH =
      Pattern.compile("textures/[a-z0-9][a-z0-9._-]{0,63}\\.png");
  private static final Set<String> FACE_NAMES =
      Set.of("north", "south", "east", "west", "up", "down");

  @Override
  public SpectraAssetDocument read(Path sourceFile) throws IOException {
    Objects.requireNonNull(sourceFile, "sourceFile");
    if (!Files.isRegularFile(sourceFile)
        || !sourceFile.getFileName().toString().endsWith(".spectra.zip")) {
      throw new IllegalArgumentException("Supported asset source must be a .spectra.zip file");
    }
    if (Files.size(sourceFile) > MAX_ARCHIVE_BYTES) {
      throw new IllegalArgumentException(
          "Asset bundle exceeds compressed size limit of 5000000 bytes");
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

  private Map<String, byte[]> readArchive(Path sourceFile) throws IOException {
    Map<String, byte[]> entries = new LinkedHashMap<>();
    Set<String> names = new HashSet<>();
    long totalBytes = 0L;
    int entryCount = 0;

    try (InputStream input = Files.newInputStream(sourceFile);
        ZipInputStream zip = new ZipInputStream(input, StandardCharsets.UTF_8)) {
      ZipEntry entry;
      while ((entry = zip.getNextEntry()) != null) {
        entryCount++;
        if (entryCount > MAX_ARCHIVE_ENTRIES) {
          throw new IllegalArgumentException("Asset bundle contains more than 18 ZIP entries");
        }
        String name = entry.getName();
        validateSafeEntryName(name);
        if (!names.add(name)) {
          throw new IllegalArgumentException("Asset bundle contains duplicate ZIP entry: " + name);
        }
        if (entry.isDirectory()) {
          if (!"textures/".equals(name)) {
            throw new IllegalArgumentException(
                "Asset bundle contains unsupported directory: " + name);
          }
          continue;
        }

        byte[] bytes = readBoundedEntry(zip, name);
        totalBytes += bytes.length;
        if (totalBytes > MAX_TOTAL_UNCOMPRESSED_BYTES) {
          throw new IllegalArgumentException(
              "Asset bundle exceeds expanded size limit of 8000000 bytes");
        }
        entries.put(name, bytes);
      }
    }
    return Map.copyOf(entries);
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
    if (!"free".equals(requiredString(meta, "model_format", MODEL_FILE))) {
      throw new IllegalArgumentException(
          "Only Blockbench Generic Model projects (model_format=free) are supported");
    }

    Map<String, SpectraAssetTexture> textures =
        parseTextures(requiredArray(root, "textures", MODEL_FILE), bundleTextures);
    Map<String, SpectraAssetGeometry> elements =
        parseElements(requiredArray(root, "elements", MODEL_FILE), textures.keySet());
    List<SpectraAssetNode> nodes =
        parseOutliner(requiredArray(root, "outliner", MODEL_FILE), elements);
    if (nodes.isEmpty()) {
      throw new IllegalArgumentException("Blockbench project must contain at least one root group");
    }
    Map<String, SpectraAssetAnimation> animations = parseAnimations(root, nodeIds(nodes));
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
      if (source.startsWith("data:image/png;base64,")) {
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
      validatePng(data, textureName);
      if (textures.putIfAbsent(textureId, new SpectraAssetTexture(textureName, data, null))
          != null) {
        throw new IllegalArgumentException(
            "Blockbench project contains duplicate texture id: " + textureId);
      }
    }
    return Map.copyOf(textures);
  }

  private Map<String, SpectraAssetGeometry> parseElements(
      JsonArray elementArray, Set<String> textureIds) {
    if (elementArray.size() > MAX_CUBES) {
      throw new IllegalArgumentException("Blockbench project contains more than 512 cubes");
    }
    Map<String, SpectraAssetGeometry> elements = new LinkedHashMap<>();
    for (JsonElement elementValue : elementArray) {
      JsonObject element = requireObject(elementValue, "elements[]");
      String id = requiredString(element, "uuid", "elements[]");
      Vector3 from = requiredVector(element, "from", "elements." + id);
      Vector3 to = requiredVector(element, "to", "elements." + id);
      Vector3 origin = optionalVector(element, "origin", Vector3.ZERO, "elements." + id);
      EulerRotation rotation =
          toEuler(optionalVector(element, "rotation", Vector3.ZERO, "elements." + id));
      float inflate = optionalFiniteFloat(element, "inflate", 0.0f, "elements." + id);
      if (from.x() >= to.x() || from.y() >= to.y() || from.z() >= to.z()) {
        throw new IllegalArgumentException(
            "Cube " + id + " must have from coordinates smaller than to coordinates");
      }
      Map<String, SpectraAssetFace> faces =
          parseFaces(requiredObject(element, "faces", "elements." + id), textureIds, id);
      if (elements.putIfAbsent(
              id, new SpectraAssetGeometry(from, to, origin, rotation, inflate, faces))
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
      JsonObject faceObject, Set<String> textureIds, String elementId) {
    Map<String, SpectraAssetFace> faces = new LinkedHashMap<>();
    for (Map.Entry<String, JsonElement> faceEntry : faceObject.entrySet()) {
      if (!FACE_NAMES.contains(faceEntry.getKey())) {
        throw new IllegalArgumentException(
            "Cube " + elementId + " has unsupported face " + faceEntry.getKey());
      }
      JsonObject face = requireObject(faceEntry.getValue(), "elements." + elementId + ".faces");
      List<Double> uv = requiredUv(face, "elements." + elementId + ".faces." + faceEntry.getKey());
      String textureRef = requiredString(face, "texture", "elements." + elementId + ".faces");
      String textureId = textureRef.startsWith("#") ? textureRef.substring(1) : textureRef;
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

  private List<SpectraAssetNode> parseOutliner(
      JsonArray outliner, Map<String, SpectraAssetGeometry> elements) {
    List<SpectraAssetNode> nodes = new ArrayList<>();
    Set<String> nodeIds = new HashSet<>();
    Set<String> referencedElements = new HashSet<>();
    for (JsonElement value : outliner) {
      nodes.add(
          parseNode(requireObject(value, "outliner[]"), elements, nodeIds, referencedElements, 1));
    }
    if (!referencedElements.equals(elements.keySet())) {
      throw new IllegalArgumentException(
          "Every Blockbench cube must belong to exactly one outliner group");
    }
    return List.copyOf(nodes);
  }

  private SpectraAssetNode parseNode(
      JsonObject group,
      Map<String, SpectraAssetGeometry> elements,
      Set<String> nodeIds,
      Set<String> referencedElements,
      int depth) {
    if (depth > 32) {
      throw new IllegalArgumentException("Blockbench group hierarchy exceeds maximum depth of 32");
    }
    if (nodeIds.size() >= MAX_NODES) {
      throw new IllegalArgumentException("Blockbench project contains more than 128 groups");
    }
    String nodeId = requiredString(group, "uuid", "outliner group");
    if (!MODEL_ID.matcher(nodeId).matches()) {
      throw new IllegalArgumentException("Blockbench group uuid must match " + MODEL_ID.pattern());
    }
    requiredString(group, "name", "outliner group");
    if (!nodeIds.add(nodeId)) {
      throw new IllegalArgumentException(
          "Blockbench project contains duplicate group uuid: " + nodeId);
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
            parseNode(child.getAsJsonObject(), elements, nodeIds, referencedElements, depth + 1));
      } else {
        throw new IllegalArgumentException(
            "Outliner children must be group objects or cube UUID strings");
      }
    }
    return new SpectraAssetNode(
        nodeId, pivot, Vector3.ZERO, rotation, Vector3.ONE, cubes, childNodes);
  }

  private Map<String, SpectraAssetAnimation> parseAnimations(JsonObject root, Set<String> nodeIds) {
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
        String nodeId = animatorEntry.getKey();
        if (!nodeIds.contains(nodeId)) {
          throw new IllegalArgumentException(
              "Animation " + name + " targets unknown group " + nodeId);
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

  private static Set<String> nodeIds(List<SpectraAssetNode> nodes) {
    Set<String> ids = new HashSet<>();
    for (SpectraAssetNode node : nodes) {
      collectNodeIds(node, ids);
    }
    return Set.copyOf(ids);
  }

  private static void collectNodeIds(SpectraAssetNode node, Set<String> ids) {
    ids.add(node.nodeId());
    node.children().forEach(child -> collectNodeIds(child, ids));
  }

  private static void validateSafeEntryName(String name) {
    if (name == null
        || name.isBlank()
        || name.startsWith("/")
        || name.startsWith("\\")
        || name.contains("\\")
        || name.contains("..")
        || Path.of(name).isAbsolute()
        || !Path.of(name).normalize().toString().replace('\\', '/').equals(name)) {
      throw new IllegalArgumentException("Asset bundle contains unsafe ZIP entry: " + name);
    }
  }

  private static byte[] readBoundedEntry(InputStream input, String name) throws IOException {
    ByteArrayOutputStream output = new ByteArrayOutputStream();
    byte[] buffer = new byte[8192];
    int read;
    while ((read = input.read(buffer)) != -1) {
      if (output.size() > MAX_ENTRY_BYTES - read) {
        throw new IllegalArgumentException("Asset bundle entry exceeds 2000000 bytes: " + name);
      }
      output.write(buffer, 0, read);
    }
    return output.toByteArray();
  }

  private static byte[] decodeEmbeddedTexture(String source) {
    try {
      return Base64.getDecoder().decode(source.substring("data:image/png;base64,".length()));
    } catch (IllegalArgumentException exception) {
      throw new IllegalArgumentException(
          "Embedded texture is not valid Base64 PNG data", exception);
    }
  }

  private static void validatePng(byte[] data, String textureName) {
    try (ImageInputStream input = ImageIO.createImageInputStream(new ByteArrayInputStream(data))) {
      Iterator<ImageReader> readers = ImageIO.getImageReaders(input);
      if (!readers.hasNext()) {
        throw new IllegalArgumentException("Texture is not a decodable image: " + textureName);
      }
      ImageReader reader = readers.next();
      try {
        reader.setInput(input, true, true);
        if (!"png".equalsIgnoreCase(reader.getFormatName())) {
          throw new IllegalArgumentException("Texture must be PNG: " + textureName);
        }
        int width = reader.getWidth(0);
        int height = reader.getHeight(0);
        if (width <= 0
            || height <= 0
            || width > MAX_TEXTURE_DIMENSION
            || height > MAX_TEXTURE_DIMENSION
            || (long) width * height > MAX_TEXTURE_PIXELS) {
          throw new IllegalArgumentException(
              "Texture dimensions exceed 1024x1024 limit: " + textureName);
        }
      } finally {
        reader.dispose();
      }
    } catch (IOException exception) {
      throw new IllegalArgumentException("Could not inspect texture " + textureName, exception);
    }
  }

  private static String decodeUtf8(byte[] bytes, String path) {
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

  private static String sha256(byte[] bytes) {
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

  private static void assertJsonDepth(String json, int maxDepth) {
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

  private static JsonObject parseObject(String json, String path) {
    try {
      return requireObject(JsonParser.parseString(json), path);
    } catch (RuntimeException exception) {
      throw new IllegalArgumentException("Invalid JSON in " + path, exception);
    }
  }

  private static JsonObject requiredObject(JsonObject parent, String name, String path) {
    if (!parent.has(name)) {
      throw new IllegalArgumentException(path + " is missing required field " + name);
    }
    return requireObject(parent.get(name), path + "." + name);
  }

  private static JsonArray requiredArray(JsonObject parent, String name, String path) {
    if (!parent.has(name) || !parent.get(name).isJsonArray()) {
      throw new IllegalArgumentException(path + "." + name + " must be an array");
    }
    return parent.getAsJsonArray(name);
  }

  private static JsonObject requireObject(JsonElement element, String path) {
    if (element == null || !element.isJsonObject()) {
      throw new IllegalArgumentException(path + " must be an object");
    }
    return element.getAsJsonObject();
  }

  private static String requiredString(JsonObject object, String name, String path) {
    if (!object.has(name)
        || !object.get(name).isJsonPrimitive()
        || !object.get(name).getAsJsonPrimitive().isString()
        || object.get(name).getAsString().isBlank()) {
      throw new IllegalArgumentException(path + "." + name + " must be a non-empty string");
    }
    return object.get(name).getAsString();
  }

  private static String optionalString(JsonObject object, String name, String defaultValue) {
    if (!object.has(name)) {
      return defaultValue;
    }
    if (!object.get(name).isJsonPrimitive() || !object.get(name).getAsJsonPrimitive().isString()) {
      throw new IllegalArgumentException(name + " must be a string");
    }
    return object.get(name).getAsString();
  }

  private static int requiredInt(JsonObject object, String name, String path) {
    if (!object.has(name)) {
      throw new IllegalArgumentException(path + "." + name + " must be an integer");
    }
    return optionalInt(object, name, 0, path);
  }

  private static int optionalInt(JsonObject object, String name, int defaultValue, String path) {
    if (!object.has(name)) {
      return defaultValue;
    }
    try {
      return object.get(name).getAsInt();
    } catch (RuntimeException exception) {
      throw new IllegalArgumentException(path + "." + name + " must be an integer", exception);
    }
  }

  private static double requiredFiniteDouble(JsonObject object, String name, String path) {
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

  private static float optionalFiniteFloat(
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

  private static Vector3 requiredVector(JsonObject object, String name, String path) {
    if (!object.has(name)) {
      throw new IllegalArgumentException(path + "." + name + " must be a three-number array");
    }
    return vector(object.get(name), path + "." + name);
  }

  private static Vector3 optionalVector(
      JsonObject object, String name, Vector3 defaultValue, String path) {
    return object.has(name) ? vector(object.get(name), path + "." + name) : defaultValue;
  }

  private static Vector3 vector(JsonElement element, String path) {
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

  private static EulerRotation toEuler(Vector3 vector) {
    return new EulerRotation(vector.x(), vector.y(), vector.z());
  }

  private static List<Double> requiredUv(JsonObject face, String path) {
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

  private static Vector3 dataPointVector(JsonObject keyframe, String path) {
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

  private static float dataPointComponent(JsonObject point, String component, String path) {
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

  private static LoopMode parseLoopMode(String value) {
    return switch (value.toLowerCase(Locale.ROOT)) {
      case "once", "" -> LoopMode.ONCE;
      case "loop" -> LoopMode.LOOP;
      case "pingpong", "ping_pong", "ping-pong" -> LoopMode.PING_PONG;
      default ->
          throw new IllegalArgumentException(
              "Unsupported Blockbench animation loop mode: " + value);
    };
  }

  private static Easing parseEasing(String value) {
    return switch (value.toLowerCase(Locale.ROOT)) {
      case "linear" -> Easing.LINEAR;
      case "step" -> Easing.STEP;
      default ->
          throw new IllegalArgumentException("Unsupported Blockbench interpolation: " + value);
    };
  }

  private static byte[] requiredEntry(Map<String, byte[]> entries, String path) {
    byte[] bytes = entries.get(path);
    if (bytes == null) {
      throw new IllegalArgumentException("Asset bundle is missing required entry: " + path);
    }
    return bytes;
  }

  private record BundleManifest(
      String modelId, Set<String> texturePaths, Map<String, String> checksums) {}
}
