package org.freakz.common.model.slack;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
class Block {
    private String type;
    private String blockId;
    private List<Element> elements;
}