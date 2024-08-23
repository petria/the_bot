package org.freakz.common.model.slack;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
class ElementDetail {
    private String type;
    private String text;
}