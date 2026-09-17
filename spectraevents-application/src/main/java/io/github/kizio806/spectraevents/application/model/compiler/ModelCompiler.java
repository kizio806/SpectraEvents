package io.github.kizio806.spectraevents.application.model.compiler;

import io.github.kizio806.spectraevents.application.config.validation.ValidationDiagnostic;
import io.github.kizio806.spectraevents.application.config.validation.ValidationDiagnostic.Severity;
import io.github.kizio806.spectraevents.application.model.spec.InteractionSpec;
import io.github.kizio806.spectraevents.application.model.spec.ModelPartSpec;
import io.github.kizio806.spectraevents.application.model.spec.ModelSpec;
import io.github.kizio806.spectraevents.application.model.spec.RenderPropertiesSpec;
import io.github.kizio806.spectraevents.application.model.spec.TransformSpec;
import io.github.kizio806.spectraevents.core.visual.model.BillboardMode;
import io.github.kizio806.spectraevents.core.visual.model.BlockAssetRef;
import io.github.kizio806.spectraevents.core.visual.model.DisplayTransformMode;
import io.github.kizio806.spectraevents.core.visual.model.InteractionDefinition;
import io.github.kizio806.spectraevents.core.visual.model.InteractionId;
import io.github.kizio806.spectraevents.core.visual.model.ItemAssetRef;
import io.github.kizio806.spectraevents.core.visual.model.ModelDefinition;
import io.github.kizio806.spectraevents.core.visual.model.ModelId;
import io.github.kizio806.spectraevents.core.visual.model.ModelPartDefinition;
import io.github.kizio806.spectraevents.core.visual.model.ModelPartId;
import io.github.kizio806.spectraevents.core.visual.model.ModelPartType;
import io.github.kizio806.spectraevents.core.visual.model.ModelRenderProperties;
import io.github.kizio806.spectraevents.core.visual.model.ModelTransform;
import io.github.kizio806.spectraevents.core.visual.model.Quaternion;
import io.github.kizio806.spectraevents.core.visual.model.TextAlignment;
import io.github.kizio806.spectraevents.core.visual.model.TextAssetRef;
import io.github.kizio806.spectraevents.core.visual.model.Vector3;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Compiles raw model authoring DTO specs into immutable, pre-calculated ModelDefinition instances.
 */
public class ModelCompiler {

  public static final int MAX_HIERARCHY_DEPTH = 32;

  public ModelDefinition compile(ModelSpec spec) {
    Objects.requireNonNull(spec, "spec cannot be null");
    List<ValidationDiagnostic> diagnostics = new ArrayList<>();

    // 1. Validate ID
    if (spec.getId() == null || spec.getId().isBlank()) {
      diagnostics.add(
          new ValidationDiagnostic(
              Severity.ERROR, "MODEL_ID_BLANK", "id", "Model ID cannot be null or blank"));
    }
    ModelId modelId = spec.getId() != null ? new ModelId(spec.getId()) : new ModelId("unknown");

    // 2. Validate Parts map
    if (spec.getParts() == null || spec.getParts().isEmpty()) {
      diagnostics.add(
          new ValidationDiagnostic(
              Severity.ERROR, "MODEL_NO_PARTS", "parts", "Model must contain at least one part"));
      throw new ModelCompilerException(
          "Failed to compile model '" + modelId.value() + "'", diagnostics);
    }

    Map<String, ModelPartSpec> partSpecs = spec.getParts();

    // Guardrail: Max parts
    if (partSpecs.size() > ModelDefinition.MAX_PARTS_LIMIT) {
      diagnostics.add(
          new ValidationDiagnostic(
              Severity.ERROR,
              "MODEL_TOO_MANY_PARTS",
              "parts",
              "Model contains "
                  + partSpecs.size()
                  + " parts, exceeding limit of "
                  + ModelDefinition.MAX_PARTS_LIMIT));
    }

    // 3. Check for missing parents and build adjacency list
    Map<String, String> parentMap = new HashMap<>();
    Map<String, List<String>> childrenMap = new HashMap<>();

    for (Map.Entry<String, ModelPartSpec> entry : partSpecs.entrySet()) {
      String partId = entry.getKey();
      ModelPartSpec partSpec = entry.getValue();

      if (partId == null || partId.isBlank()) {
        diagnostics.add(
            new ValidationDiagnostic(
                Severity.ERROR, "PART_ID_BLANK", "parts", "Part ID cannot be blank"));
        continue;
      }

      String parentId = partSpec.getParent();
      if (parentId != null && !parentId.isBlank()) {
        if (!partSpecs.containsKey(parentId)) {
          diagnostics.add(
              new ValidationDiagnostic(
                  Severity.ERROR,
                  "UNKNOWN_PARENT_PART",
                  "parts." + partId + ".parent",
                  "Parent part '"
                      + parentId
                      + "' does not exist in model '"
                      + modelId.value()
                      + "'"));
        } else {
          parentMap.put(partId, parentId);
          childrenMap.computeIfAbsent(parentId, k -> new ArrayList<>()).add(partId);
        }
      }
    }

    // 4. Cycle detection & Hierarchy depth check
    detectCyclesAndDepth(partSpecs.keySet(), parentMap, modelId.value(), diagnostics);

    if (diagnostics.stream().anyMatch(d -> d.severity() == Severity.ERROR)) {
      throw new ModelCompilerException(
          "Failed to compile model '" + modelId.value() + "'", diagnostics);
    }

    // 5. Topological sort for composed transform pre-calculation
    List<String> sortedPartIds = topologicalSort(partSpecs.keySet(), parentMap);
    Map<String, ModelTransform> composedTransformMap = new HashMap<>();
    Map<String, ModelPartDefinition> compiledPartsMap = new HashMap<>();
    List<ModelPartDefinition> compiledParts = new ArrayList<>();

    for (String partId : sortedPartIds) {
      ModelPartSpec partSpec = partSpecs.get(partId);
      ModelPartId pId = ModelPartId.of(partId);
      String pParentStr = parentMap.get(partId);
      ModelPartId pParentId = pParentStr != null ? ModelPartId.of(pParentStr) : null;

      ModelTransform localTransform =
          compileTransform(partSpec.getTransform(), "parts." + partId + ".transform", diagnostics);

      ModelTransform composedTransform;
      if (pParentStr == null) {
        composedTransform = localTransform;
      } else {
        ModelTransform parentComposed = composedTransformMap.get(pParentStr);
        composedTransform = localTransform.compose(parentComposed);
      }
      composedTransformMap.put(partId, composedTransform);

      ModelPartType partType =
          parsePartType(partSpec.getType(), "parts." + partId + ".type", diagnostics);
      ModelRenderProperties renderProperties =
          compileRenderProperties(partSpec.getRender(), "parts." + partId + ".render", diagnostics);
      Object visualAsset = compileVisualAsset(partType, partSpec, "parts." + partId, diagnostics);

      ModelPartDefinition compiledPart =
          new ModelPartDefinition(
              pId,
              pParentId,
              partType,
              localTransform,
              composedTransform,
              renderProperties,
              visualAsset);
      compiledPartsMap.put(partId, compiledPart);
      compiledParts.add(compiledPart);
    }

    // 6. Compile Interactions
    List<InteractionDefinition> compiledInteractions = new ArrayList<>();
    if (spec.getInteractions() != null) {
      if (spec.getInteractions().size() > ModelDefinition.MAX_INTERACTIONS_LIMIT) {
        diagnostics.add(
            new ValidationDiagnostic(
                Severity.ERROR,
                "TOO_MANY_INTERACTIONS",
                "interactions",
                "Interactions count exceeds limit of " + ModelDefinition.MAX_INTERACTIONS_LIMIT));
      }

      for (Map.Entry<String, InteractionSpec> entry : spec.getInteractions().entrySet()) {
        String iIdStr = entry.getKey();
        InteractionSpec iSpec = entry.getValue();

        if (iIdStr == null || iIdStr.isBlank()) {
          diagnostics.add(
              new ValidationDiagnostic(
                  Severity.ERROR,
                  "INTERACTION_ID_BLANK",
                  "interactions",
                  "Interaction ID cannot be blank"));
          continue;
        }

        InteractionId iId = InteractionId.of(iIdStr);
        String pParentStr = iSpec.getParent();
        ModelPartId pParentId = null;

        Vector3 localOffset =
            parseVector3(
                iSpec.getOffset(), Vector3.ZERO, "interactions." + iIdStr + ".offset", diagnostics);

        if (pParentStr != null && !pParentStr.isBlank()) {
          if (!partSpecs.containsKey(pParentStr)) {
            diagnostics.add(
                new ValidationDiagnostic(
                    Severity.ERROR,
                    "UNKNOWN_INTERACTION_PARENT",
                    "interactions." + iIdStr + ".parent",
                    "Interaction parent part '" + pParentStr + "' does not exist"));
          } else {
            pParentId = ModelPartId.of(pParentStr);
          }
        }

        Vector3 composedOffset;
        if (pParentStr != null && composedTransformMap.containsKey(pParentStr)) {
          ModelTransform parentTransform = composedTransformMap.get(pParentStr);
          composedOffset =
              parentTransform.translation().add(parentTransform.rotation().transform(localOffset));
        } else {
          composedOffset = localOffset;
        }

        float width = iSpec.getWidth() != null ? iSpec.getWidth() : 1.0f;
        float height = iSpec.getHeight() != null ? iSpec.getHeight() : 1.0f;
        boolean responsive = iSpec.getResponsive() == null || iSpec.getResponsive();

        if (width <= 0.0f) {
          diagnostics.add(
              new ValidationDiagnostic(
                  Severity.ERROR,
                  "INVALID_INTERACTION_WIDTH",
                  "interactions." + iIdStr + ".width",
                  "Interaction width must be positive: " + width));
        }
        if (height <= 0.0f) {
          diagnostics.add(
              new ValidationDiagnostic(
                  Severity.ERROR,
                  "INVALID_INTERACTION_HEIGHT",
                  "interactions." + iIdStr + ".height",
                  "Interaction height must be positive: " + height));
        }

        if (diagnostics.stream().noneMatch(d -> d.severity() == Severity.ERROR)) {
          compiledInteractions.add(
              new InteractionDefinition(
                  iId, pParentId, localOffset, composedOffset, width, height, responsive));
        }
      }
    }

    if (diagnostics.stream().anyMatch(d -> d.severity() == Severity.ERROR)) {
      throw new ModelCompilerException(
          "Failed to compile model '" + modelId.value() + "'", diagnostics);
    }

    return new ModelDefinition(modelId, compiledParts, compiledInteractions);
  }

  private void detectCyclesAndDepth(
      Set<String> allPartIds,
      Map<String, String> parentMap,
      String modelId,
      List<ValidationDiagnostic> diagnostics) {

    for (String startPartId : allPartIds) {
      Set<String> visitedInPath = new LinkedHashSet<>();
      String current = startPartId;
      int depth = 0;

      while (current != null) {
        if (!visitedInPath.add(current)) {
          diagnostics.add(
              new ValidationDiagnostic(
                  Severity.ERROR,
                  "HIERARCHY_CYCLE",
                  "parts." + startPartId,
                  "Hierarchy cycle detected involving part path: " + visitedInPath));
          break;
        }
        depth++;
        if (depth > MAX_HIERARCHY_DEPTH) {
          diagnostics.add(
              new ValidationDiagnostic(
                  Severity.ERROR,
                  "HIERARCHY_TOO_DEEP",
                  "parts." + startPartId,
                  "Hierarchy depth exceeds limit of " + MAX_HIERARCHY_DEPTH));
          break;
        }
        current = parentMap.get(current);
      }
    }
  }

  private List<String> topologicalSort(Set<String> allPartIds, Map<String, String> parentMap) {
    List<String> sorted = new ArrayList<>();
    Set<String> visited = new HashSet<>();

    for (String partId : allPartIds) {
      visitTopological(partId, parentMap, visited, sorted);
    }
    return sorted;
  }

  private void visitTopological(
      String partId, Map<String, String> parentMap, Set<String> visited, List<String> sorted) {
    if (visited.contains(partId)) {
      return;
    }
    String parentId = parentMap.get(partId);
    if (parentId != null) {
      visitTopological(parentId, parentMap, visited, sorted);
    }
    visited.add(partId);
    sorted.add(partId);
  }

  private ModelTransform compileTransform(
      TransformSpec spec, String path, List<ValidationDiagnostic> diagnostics) {
    if (spec == null) {
      return ModelTransform.IDENTITY;
    }

    Vector3 translation =
        parseVector3(spec.getTranslation(), Vector3.ZERO, path + ".translation", diagnostics);
    Vector3 scale = parseVector3(spec.getScale(), Vector3.ONE, path + ".scale", diagnostics);
    Vector3 pivot = parseVector3(spec.getPivot(), Vector3.ZERO, path + ".pivot", diagnostics);
    Quaternion rotation = compileRotation(spec.getRotation(), path + ".rotation", diagnostics);

    try {
      return new ModelTransform(translation, rotation, scale, pivot);
    } catch (IllegalArgumentException e) {
      diagnostics.add(
          new ValidationDiagnostic(Severity.ERROR, "INVALID_TRANSFORM", path, e.getMessage()));
      return ModelTransform.IDENTITY;
    }
  }

  private Quaternion compileRotation(
      TransformSpec.RotationSpec spec, String path, List<ValidationDiagnostic> diagnostics) {
    if (spec == null) {
      return Quaternion.IDENTITY;
    }

    if (spec.getEuler() != null && !spec.getEuler().isEmpty()) {
      if (spec.getEuler().size() != 3) {
        diagnostics.add(
            new ValidationDiagnostic(
                Severity.ERROR,
                "INVALID_EULER_ROTATION",
                path + ".euler",
                "Euler rotation must contain 3 float elements [pitch, yaw, roll]"));
        return Quaternion.IDENTITY;
      }
      return Quaternion.fromEulerDegrees(
          spec.getEuler().get(0), spec.getEuler().get(1), spec.getEuler().get(2));
    }

    if (spec.getQuaternion() != null && !spec.getQuaternion().isEmpty()) {
      if (spec.getQuaternion().size() != 4) {
        diagnostics.add(
            new ValidationDiagnostic(
                Severity.ERROR,
                "INVALID_QUATERNION_ROTATION",
                path + ".quaternion",
                "Quaternion rotation must contain 4 float elements [x, y, z, w]"));
        return Quaternion.IDENTITY;
      }
      return Quaternion.of(
          spec.getQuaternion().get(0),
          spec.getQuaternion().get(1),
          spec.getQuaternion().get(2),
          spec.getQuaternion().get(3));
    }

    return Quaternion.IDENTITY;
  }

  private Vector3 parseVector3(
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

  private ModelPartType parsePartType(
      String typeStr, String path, List<ValidationDiagnostic> diagnostics) {
    if (typeStr == null || typeStr.isBlank()) {
      diagnostics.add(
          new ValidationDiagnostic(
              Severity.ERROR, "PART_TYPE_BLANK", path, "Part type cannot be blank"));
      return ModelPartType.ITEM_DISPLAY;
    }
    try {
      return ModelPartType.valueOf(typeStr.toUpperCase(Locale.ROOT));
    } catch (IllegalArgumentException e) {
      diagnostics.add(
          new ValidationDiagnostic(
              Severity.ERROR,
              "UNKNOWN_PART_TYPE",
              path,
              "Unknown part type '"
                  + typeStr
                  + "'. Expected ITEM_DISPLAY, BLOCK_DISPLAY, or TEXT_DISPLAY"));
      return ModelPartType.ITEM_DISPLAY;
    }
  }

  private ModelRenderProperties compileRenderProperties(
      RenderPropertiesSpec spec, String path, List<ValidationDiagnostic> diagnostics) {
    if (spec == null) {
      return ModelRenderProperties.DEFAULT;
    }

    BillboardMode billboard = BillboardMode.FIXED;
    if (spec.getBillboard() != null) {
      try {
        billboard = BillboardMode.valueOf(spec.getBillboard().toUpperCase(Locale.ROOT));
      } catch (IllegalArgumentException e) {
        diagnostics.add(
            new ValidationDiagnostic(
                Severity.ERROR,
                "UNKNOWN_BILLBOARD_MODE",
                path + ".billboard",
                "Unknown billboard mode: " + spec.getBillboard()));
      }
    }

    int bBlock = spec.getBrightnessBlock() != null ? spec.getBrightnessBlock() : -1;
    int bSky = spec.getBrightnessSky() != null ? spec.getBrightnessSky() : -1;
    float shadowR = spec.getShadowRadius() != null ? spec.getShadowRadius() : 0.0f;
    float shadowS = spec.getShadowStrength() != null ? spec.getShadowStrength() : 1.0f;
    float viewR = spec.getViewRange() != null ? spec.getViewRange() : 1.0f;
    float dWidth = spec.getDisplayWidth() != null ? spec.getDisplayWidth() : 0.0f;
    float dHeight = spec.getDisplayHeight() != null ? spec.getDisplayHeight() : 0.0f;
    String glow = spec.getGlowColor();
    int interpDelay = spec.getInterpolationDelay() != null ? spec.getInterpolationDelay() : 0;
    int interpDur = spec.getInterpolationDuration() != null ? spec.getInterpolationDuration() : 0;
    int teleDur = spec.getTeleportDuration() != null ? spec.getTeleportDuration() : 0;

    try {
      return new ModelRenderProperties(
          billboard,
          bBlock,
          bSky,
          shadowR,
          shadowS,
          viewR,
          dWidth,
          dHeight,
          glow,
          interpDelay,
          interpDur,
          teleDur);
    } catch (IllegalArgumentException e) {
      diagnostics.add(
          new ValidationDiagnostic(
              Severity.ERROR, "INVALID_RENDER_PROPERTIES", path, e.getMessage()));
      return ModelRenderProperties.DEFAULT;
    }
  }

  private Object compileVisualAsset(
      ModelPartType partType,
      ModelPartSpec spec,
      String path,
      List<ValidationDiagnostic> diagnostics) {
    switch (partType) {
      case ITEM_DISPLAY -> {
        String itemStr = spec.getItem();
        if (itemStr == null || itemStr.isBlank()) {
          diagnostics.add(
              new ValidationDiagnostic(
                  Severity.ERROR,
                  "ITEM_DISPLAY_MISSING_ITEM",
                  path + ".item",
                  "ITEM_DISPLAY part requires 'item' property"));
          return ItemAssetRef.of("minecraft:stone");
        }
        DisplayTransformMode mode = DisplayTransformMode.NONE;
        if (spec.getTransformMode() != null) {
          try {
            mode = DisplayTransformMode.valueOf(spec.getTransformMode().toUpperCase(Locale.ROOT));
          } catch (IllegalArgumentException e) {
            diagnostics.add(
                new ValidationDiagnostic(
                    Severity.ERROR,
                    "UNKNOWN_TRANSFORM_MODE",
                    path + ".transformMode",
                    "Unknown display transform mode: " + spec.getTransformMode()));
          }
        }
        return new ItemAssetRef(itemStr, mode);
      }
      case BLOCK_DISPLAY -> {
        String blockStr = spec.getBlock();
        if (blockStr == null || blockStr.isBlank()) {
          diagnostics.add(
              new ValidationDiagnostic(
                  Severity.ERROR,
                  "BLOCK_DISPLAY_MISSING_BLOCK",
                  path + ".block",
                  "BLOCK_DISPLAY part requires 'block' property"));
          return new BlockAssetRef("minecraft:stone");
        }
        return new BlockAssetRef(blockStr);
      }
      case TEXT_DISPLAY -> {
        String textStr = spec.getText() != null ? spec.getText() : "";
        TextAlignment alignment = TextAlignment.CENTER;
        if (spec.getTextAlignment() != null) {
          try {
            alignment = TextAlignment.valueOf(spec.getTextAlignment().toUpperCase(Locale.ROOT));
          } catch (IllegalArgumentException e) {
            diagnostics.add(
                new ValidationDiagnostic(
                    Severity.ERROR,
                    "UNKNOWN_TEXT_ALIGNMENT",
                    path + ".textAlignment",
                    "Unknown text alignment: " + spec.getTextAlignment()));
          }
        }
        int lineW = spec.getLineWidth() != null ? spec.getLineWidth() : 200;
        int bgCol = spec.getBackgroundColor() != null ? spec.getBackgroundColor() : 0;
        int textOp = spec.getTextOpacity() != null ? spec.getTextOpacity() : 255;
        boolean shadow = spec.getShadow() == null || spec.getShadow();
        boolean seeThrough = spec.getSeeThrough() != null && spec.getSeeThrough();

        return new TextAssetRef(textStr, alignment, lineW, bgCol, textOp, shadow, seeThrough);
      }
      default -> throw new IllegalStateException("Unhandled part type: " + partType);
    }
  }
}
