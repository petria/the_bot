package org.freakz.common.model.json.connectionmanager;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class BotConnectionChannelResponse {

    private String id;

    private String type;

    private String network;
    private String name;

    private String targetAlias;
}
