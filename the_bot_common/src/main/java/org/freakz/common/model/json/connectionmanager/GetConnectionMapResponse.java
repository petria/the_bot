package org.freakz.common.model.json.connectionmanager;

import lombok.Data;

import java.util.Map;

@Data
public class GetConnectionMapResponse {

    private Map<Integer, BotConnectionResponse> connectionMap;
}
