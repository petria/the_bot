package org.freakz.common.model.slack;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
class Block {
    private String type;
    private String blockId;
    private List<Element> elements;
}