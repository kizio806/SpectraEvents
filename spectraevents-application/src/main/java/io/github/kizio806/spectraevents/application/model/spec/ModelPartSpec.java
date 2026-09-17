package io.github.kizio806.spectraevents.application.model.spec;

/** Authoring DTO spec for a single part in a 3D model definition YAML. */
public class ModelPartSpec {
  private String type;
  private String parent;
  private String item;
  private String transformMode;
  private String block;
  private String text;
  private String textAlignment;
  private Integer lineWidth;
  private Integer backgroundColor;
  private Integer textOpacity;
  private Boolean shadow;
  private Boolean seeThrough;
  private TransformSpec transform;
  private RenderPropertiesSpec render;

  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  public String getParent() {
    return parent;
  }

  public void setParent(String parent) {
    this.parent = parent;
  }

  public String getItem() {
    return item;
  }

  public void setItem(String item) {
    this.item = item;
  }

  public String getTransformMode() {
    return transformMode;
  }

  public void setTransformMode(String transformMode) {
    this.transformMode = transformMode;
  }

  public String getBlock() {
    return block;
  }

  public void setBlock(String block) {
    this.block = block;
  }

  public String getText() {
    return text;
  }

  public void setText(String text) {
    this.text = text;
  }

  public String getTextAlignment() {
    return textAlignment;
  }

  public void setTextAlignment(String textAlignment) {
    this.textAlignment = textAlignment;
  }

  public Integer getLineWidth() {
    return lineWidth;
  }

  public void setLineWidth(Integer lineWidth) {
    this.lineWidth = lineWidth;
  }

  public Integer getBackgroundColor() {
    return backgroundColor;
  }

  public void setBackgroundColor(Integer backgroundColor) {
    this.backgroundColor = backgroundColor;
  }

  public Integer getTextOpacity() {
    return textOpacity;
  }

  public void setTextOpacity(Integer textOpacity) {
    this.textOpacity = textOpacity;
  }

  public Boolean getShadow() {
    return shadow;
  }

  public void setShadow(Boolean shadow) {
    this.shadow = shadow;
  }

  public Boolean getSeeThrough() {
    return seeThrough;
  }

  public void setSeeThrough(Boolean seeThrough) {
    this.seeThrough = seeThrough;
  }

  public TransformSpec getTransform() {
    return transform;
  }

  public void setTransform(TransformSpec transform) {
    this.transform = transform;
  }

  public RenderPropertiesSpec getRender() {
    return render;
  }

  public void setRender(RenderPropertiesSpec render) {
    this.render = render;
  }
}
