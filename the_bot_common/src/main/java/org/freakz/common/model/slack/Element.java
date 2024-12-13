package org.freakz.common.model.slack;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
class Element {
  private String type;
  private List<ElementDetail> elements;
}
