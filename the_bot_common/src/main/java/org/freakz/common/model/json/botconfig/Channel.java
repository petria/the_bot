package org.freakz.common.model.json.botconfig;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@AllArgsConstructor
@Builder
@Data
@NoArgsConstructor
public class Channel {

    private String id;

    private String description;

    private String name;

    private String type;

    private List<String> echoToAliases;

}
