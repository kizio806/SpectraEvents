package io.github.kizio806.spectraevents.application.model.spec;

/** Authoring DTO spec for display entity render properties in YAML. */
public class RenderPropertiesSpec {
  private String billboard;
  private Integer brightnessBlock;
  private Integer brightnessSky;
  private Float shadowRadius;
  private Float shadowStrength;
  private Float viewRange;
  private Float displayWidth;
  private Float displayHeight;
  private String glowColor;
  private Integer interpolationDelay;
  private Integer interpolationDuration;
  private Integer teleportDuration;

  public String getBillboard() {
    return billboard;
  }

  public void setBillboard(String billboard) {
    this.billboard = billboard;
  }

  public Integer getBrightnessBlock() {
    return brightnessBlock;
  }

  public void setBrightnessBlock(Integer brightnessBlock) {
    this.brightnessBlock = brightnessBlock;
  }

  public Integer getBrightnessSky() {
    return brightnessSky;
  }

  public void setBrightnessSky(Integer brightnessSky) {
    this.brightnessSky = brightnessSky;
  }

  public Float getShadowRadius() {
    return shadowRadius;
  }

  public void setShadowRadius(Float shadowRadius) {
    this.shadowRadius = shadowRadius;
  }

  public Float getShadowStrength() {
    return shadowStrength;
  }

  public void setShadowStrength(Float shadowStrength) {
    this.shadowStrength = shadowStrength;
  }

  public Float getViewRange() {
    return viewRange;
  }

  public void setViewRange(Float viewRange) {
    this.viewRange = viewRange;
  }

  public Float getDisplayWidth() {
    return displayWidth;
  }

  public void setDisplayWidth(Float displayWidth) {
    this.displayWidth = displayWidth;
  }

  public Float getDisplayHeight() {
    return displayHeight;
  }

  public void setDisplayHeight(Float displayHeight) {
    this.displayHeight = displayHeight;
  }

  public String getGlowColor() {
    return glowColor;
  }

  public void setGlowColor(String glowColor) {
    this.glowColor = glowColor;
  }

  public Integer getInterpolationDelay() {
    return interpolationDelay;
  }

  public void setInterpolationDelay(Integer interpolationDelay) {
    this.interpolationDelay = interpolationDelay;
  }

  public Integer getInterpolationDuration() {
    return interpolationDuration;
  }

  public void setInterpolationDuration(Integer interpolationDuration) {
    this.interpolationDuration = interpolationDuration;
  }

  public Integer getTeleportDuration() {
    return teleportDuration;
  }

  public void setTeleportDuration(Integer teleportDuration) {
    this.teleportDuration = teleportDuration;
  }
}
