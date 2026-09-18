package io.github.kizio806.spectraevents.adapter.blockbench;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.github.kizio806.spectraevents.application.port.AssetImportPort;
import io.github.kizio806.spectraevents.core.visual.asset.SpectraAssetAnimation;
import io.github.kizio806.spectraevents.core.visual.asset.SpectraAssetDocument;
import io.github.kizio806.spectraevents.core.visual.asset.SpectraAssetGeometry;
import io.github.kizio806.spectraevents.core.visual.asset.SpectraAssetNode;
import io.github.kizio806.spectraevents.core.visual.asset.SpectraAssetTexture;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BlockbenchProjectReader implements AssetImportPort {

  @Override
  public SpectraAssetDocument read(String jsonContent, String modelId) {
    JsonObject root = JsonParser.parseString(jsonContent).getAsJsonObject();

    // 1. Version Validation
    JsonObject meta = root.getAsJsonObject("meta");
    if (meta == null) {
      throw new IllegalArgumentException("Invalid bbmodel: missing 'meta' object");
    }
    String formatVersion =
        meta.has("format_version") ? meta.get("format_version").getAsString() : "";
    if (!formatVersion.startsWith("4.") && !formatVersion.startsWith("5.")) {
      throw new IllegalArgumentException(
          "Unsupported Blockbench format version: "
              + formatVersion
              + ". Only 4.x and 5.x are supported.");
    }

    // 2. Textures
    Map<String, SpectraAssetTexture> textures = new HashMap<>();
    JsonArray texturesArray = root.getAsJsonArray("textures");
    if (texturesArray != null) {
      for (JsonElement el : texturesArray) {
        JsonObject tex = el.getAsJsonObject();
        String id = tex.get("id").getAsString();
        String name = tex.get("name").getAsString();
        String source = tex.has("source") ? tex.get("source").getAsString() : "";
        byte[] data = null;
        if (source.startsWith("data:image")) {
          String base64 = source.substring(source.indexOf(",") + 1);
          data = Base64.getDecoder().decode(base64);
          source = null; // Embedded
        }
        textures.put(id, new SpectraAssetTexture(name, data, source));
      }
    }

    // 3. Hierarchy (Outliner)
    JsonArray outliner = root.getAsJsonArray("outliner");
    List<SpectraAssetNode> nodes = new ArrayList<>();
    if (outliner != null) {
      for (JsonElement el : outliner) {
        if (el.isJsonObject()) {
          nodes.add(parseNode(el.getAsJsonObject(), root));
        }
      }
    }

    // 4. Animations
    Map<String, SpectraAssetAnimation> animations = new HashMap<>();
    // Basic mapping implemented separately.

    return new SpectraAssetDocument(1, modelId, textures, nodes, animations);
  }

  private SpectraAssetNode parseNode(JsonObject nodeJson, JsonObject root) {
    String name = nodeJson.get("name").getAsString();

    // Blockbench pivot
    JsonArray originArr = nodeJson.getAsJsonArray("origin");
    io.github.kizio806.spectraevents.core.visual.model.Vector3 pivot =
        originArr != null
            ? new io.github.kizio806.spectraevents.core.visual.model.Vector3(
                originArr.get(0).getAsFloat(),
                originArr.get(1).getAsFloat(),
                originArr.get(2).getAsFloat())
            : io.github.kizio806.spectraevents.core.visual.model.Vector3.ZERO;

    List<SpectraAssetNode> children = new ArrayList<>();
    List<SpectraAssetGeometry> cubes = new ArrayList<>();

    JsonArray childrenArr = nodeJson.getAsJsonArray("children");
    if (childrenArr != null) {
      for (JsonElement child : childrenArr) {
        if (child.isJsonObject()) {
          children.add(parseNode(child.getAsJsonObject(), root));
        } else if (child.isJsonPrimitive()) {
          // String UUID referencing an element (cube)
          String cubeId = child.getAsString();
          // Find cube in elements...
        }
      }
    }

    return new SpectraAssetNode(
        name,
        pivot,
        io.github.kizio806.spectraevents.core.visual.model.Vector3.ZERO,
        io.github.kizio806.spectraevents.core.visual.model.EulerRotation.ZERO,
        new io.github.kizio806.spectraevents.core.visual.model.Vector3(1, 1, 1),
        cubes,
        children);
  }
}
