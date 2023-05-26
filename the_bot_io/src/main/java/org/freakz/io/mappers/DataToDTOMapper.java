package org.freakz.io.mappers;

import org.freakz.common.model.json.connectionmanager.BotConnectionChannelResponse;
import org.freakz.common.model.json.connectionmanager.BotConnectionResponse;
import org.freakz.common.model.json.connectionmanager.GetConnectionMapResponse;
import org.freakz.io.connections.BotConnection;
import org.freakz.io.connections.BotConnectionChannel;

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

// TODO        botConnection.getChannelMap().values().stream().map(this::toBotConnectionChannelResponse).forEach(bc -> response.getChannels().add(bc));

        return response;
    }

    private BotConnectionChannelResponse toBotConnectionChannelResponse(BotConnectionChannel botConnectionChannel) {
        BotConnectionChannelResponse response = new BotConnectionChannelResponse();
        response.setId(botConnectionChannel.getId());
        response.setName(botConnectionChannel.getName());
        response.setNetwork(botConnectionChannel.getNetwork());
        response.setType(botConnectionChannel.getType());
        response.setEchoToAlias((botConnectionChannel.getEchoToAlias()));
        return response;
    }
}
