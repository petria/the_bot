package org.freakz.common.model.connectionmanager;

import java.util.ArrayList;
import java.util.List;
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

    private List<BotConnectionChannelResponse> channels = new ArrayList<>();

}
