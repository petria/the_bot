package org.freakz.engine.services.urls;

import org.jsoup.nodes.Attribute;
import org.jsoup.nodes.Attributes;
import org.jsoup.nodes.Element;

import java.util.ArrayList;
import java.util.List;

public class MetaAttribute {

  private String name;
  private String value;

  public MetaAttribute() {
  }

  public MetaAttribute(String name, String value) {
    this.name = name;
    this.value = value;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getValue() {
    return value;
  }

  public void setValue(String value) {
    this.value = value;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    MetaAttribute that = (MetaAttribute) o;

    if (name != null ? !name.equals(that.name) : that.name != null) return false;
    return value != null ? value.equals(that.value) : that.value == null;
  }

  @Override
  public int hashCode() {
    int result = name != null ? name.hashCode() : 0;
    result = 31 * result + (value != null ? value.hashCode() : 0);
    return result;
  }

  @Override
  public String toString() {
    return "MetaAttribute{" +
        "name='" + name + '\'' +
        ", value='" + value + '\'' +
        '}';
  }

  public static List<MetaAttribute> fromJsoupElements(Element element) {
    List<MetaAttribute> metaAttributes = new ArrayList<>();

    Attributes attributes = element.attributes();
    String content = attributes.get("content");
    String name1 = attributes.get("name");
    String property = attributes.get("property");
    if (!name1.isEmpty()) {
      MetaAttribute metaAttribute = new MetaAttribute(name1, content);
      metaAttributes.add(metaAttribute);
    } else if (!property.isEmpty()) {
      MetaAttribute metaAttribute = new MetaAttribute(property, content);
      metaAttributes.add(metaAttribute);
    } else {
      List<Attribute> list = attributes.asList();
      if (!attributes.get("charset").isEmpty()) {
        MetaAttribute metaAttribute = new MetaAttribute("charset", content);
        metaAttributes.add(metaAttribute);
      }
      int foo = 0;

    }

    return metaAttributes;
  }
}
