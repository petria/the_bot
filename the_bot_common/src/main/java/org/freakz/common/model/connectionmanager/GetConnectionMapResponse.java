package org.freakz.common.model.connectionmanager;

import java.util.Map;
import lombok.Data;

@Data
public class GetConnectionMapResponse {

    private Map<Integer, BotConnectionResponse> connectionMap;
}
