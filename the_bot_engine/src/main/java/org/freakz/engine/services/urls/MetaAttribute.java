package org.freakz.engine.services.urls;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.jsoup.nodes.Attribute;
import org.jsoup.nodes.Attributes;
import org.jsoup.nodes.Element;

import java.util.ArrayList;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class MetaAttribute {

  private String name;
  private String value;

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
