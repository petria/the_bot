package org.freakz.common.model.json.connectionmanager;

import lombok.Data;
import org.freakz.common.dto.BotConnection;

import java.util.Map;

@Data
public class GetConnectionMapResponse {

    private Map<Integer, BotConnection> connectionMap;
}
