package org.freakz.common.model.json.connectionmanager;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BotConnectionResponse {

    private int id;

    private String type;

    private String network;

}
