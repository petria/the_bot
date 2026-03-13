package org.freakz.common.model.slack;

import java.util.List;
import java.util.Objects;

class Block {
  private String type;
  private String blockId;
  private List<Element> elements;

  public Block() {
  }

  public Block(String type, String blockId, List<Element> elements) {
    this.type = type;
    this.blockId = blockId;
    this.elements = elements;
  }

  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  public String getBlockId() {
    return blockId;
  }

  public void setBlockId(String blockId) {
    this.blockId = blockId;
  }

  public List<Element> getElements() {
    return elements;
  }

  public void setElements(List<Element> elements) {
    this.elements = elements;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    Block block = (Block) o;
    return Objects.equals(type, block.type) && Objects.equals(blockId, block.blockId) && Objects.equals(elements, block.elements);
  }

  @Override
  public int hashCode() {
    return Objects.hash(type, blockId, elements);
  }

  @Override
  public String toString() {
    return "Block{" +
        "type='" + type + '\'' +
        ", blockId='" + blockId + '\'' +
        ", elements=" + elements +
        '}';
  }
}