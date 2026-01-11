package org.freakz.common.model.slack;

import java.util.Objects;

class ElementDetail {
  private String type;
  private String text;

  public ElementDetail() {
  }

  public ElementDetail(String type, String text) {
    this.type = type;
    this.text = text;
  }

  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  public String getText() {
    return text;
  }

  public void setText(String text) {
    this.text = text;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    ElementDetail that = (ElementDetail) o;
    return Objects.equals(type, that.type) && Objects.equals(text, that.text);
  }

  @Override
  public int hashCode() {
    return Objects.hash(type, text);
  }

  @Override
  public String toString() {
    return "ElementDetail{" +
        "type='" + type + '\'' +
        ", text='" + text + '\'' +
        '}';
  }
}
