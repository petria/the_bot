package org.freakz.common.model.slack;

import java.util.List;
import java.util.Objects;

class Element {
    private String type;
    private List<ElementDetail> elements;

    public Element() {
    }

    public Element(String type, List<ElementDetail> elements) {
        this.type = type;
        this.elements = elements;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public List<ElementDetail> getElements() {
        return elements;
    }

    public void setElements(List<ElementDetail> elements) {
        this.elements = elements;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Element element = (Element) o;
        return Objects.equals(type, element.type) && Objects.equals(elements, element.elements);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, elements);
    }

    @Override
    public String toString() {
        return "Element{" +
                "type='" + type + '\'' +
                ", elements=" + elements +
                '}';
    }
}
