package io.github.kizio806.spectraevents.core.visual.model;

import java.util.Objects;

/**
 * Platform-neutral text display reference supporting MiniMessage format strings and rendering
 * options.
 */
public record TextAssetRef(
    String text,
    TextAlignment alignment,
    int lineWidth,
    int backgroundColor,
    int textOpacity,
    boolean shadow,
    boolean seeThrough) {
  public TextAssetRef {
    Objects.requireNonNull(text, "text cannot be null");
    Objects.requireNonNull(alignment, "alignment cannot be null");
  }

  public static TextAssetRef of(String text) {
    return new TextAssetRef(text, TextAlignment.CENTER, 200, 0, 255, true, false);
  }
}
