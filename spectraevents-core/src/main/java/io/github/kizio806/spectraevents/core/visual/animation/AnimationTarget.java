package io.github.kizio806.spectraevents.core.visual.animation;

import io.github.kizio806.spectraevents.core.visual.model.ModelPartId;
import java.util.Objects;

/**
 * Domain target of an animation track: either root model transform or a specific child model part.
 */
public record AnimationTarget(boolean isRoot, ModelPartId partId) {
  public static final AnimationTarget ROOT = new AnimationTarget(true, null);

  public AnimationTarget {
    if (isRoot && partId != null) {
      throw new IllegalArgumentException("Root animation target cannot specify a part ID");
    }
    if (!isRoot && partId == null) {
      throw new IllegalArgumentException("Part animation target must specify a non-null part ID");
    }
  }

  public static AnimationTarget ofPart(ModelPartId partId) {
    Objects.requireNonNull(partId, "partId cannot be null");
    return new AnimationTarget(false, partId);
  }

  public static AnimationTarget parse(String targetStr) {
    Objects.requireNonNull(targetStr, "targetStr cannot be null");
    if ("root".equalsIgnoreCase(targetStr.trim())) {
      return ROOT;
    }
    return ofPart(ModelPartId.of(targetStr.trim()));
  }

  public String name() {
    return isRoot ? "root" : partId.value();
  }
}
