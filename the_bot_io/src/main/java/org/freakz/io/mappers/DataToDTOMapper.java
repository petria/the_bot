package org.freakz.io.mappers;

import org.freakz.common.model.json.connectionmanager.BotConnectionResponse;
import org.freakz.common.model.json.connectionmanager.GetConnectionMapResponse;
import org.freakz.io.connections.BotConnection;

import java.util.HashMap;
import java.util.Map;

public class DataToDTOMapper {
    public GetConnectionMapResponse toGetConnectionMapResponse(Map<Integer, BotConnection> connectionMap) {
        GetConnectionMapResponse response = new GetConnectionMapResponse();

        Map<Integer, BotConnectionResponse> map = new HashMap<>();
        connectionMap.values().stream().map(this::toBotConnectionResponse).forEach(bc -> map.put(bc.getId(), bc));

        response.setConnectionMap(map);

        return response;
    }

    private BotConnectionResponse toBotConnectionResponse(BotConnection botConnection) {
        BotConnectionResponse response = new BotConnectionResponse();
        response.setId(botConnection.getId());
        response.setNetwork(botConnection.getNetwork());
        response.setType(botConnection.getType().name());
        return response;
    }
}
