package org.freakz.dto;

import lombok.Builder;
import lombok.Data;
import lombok.ToString;
import org.freakz.common.model.json.connectionmanager.BotConnectionResponse;
import org.freakz.services.ServiceResponse;

import java.util.Map;

@Builder
@Data
@ToString
public class ConnectionsResponse extends ServiceResponse {

    private Map<Integer, BotConnectionResponse> connectionMap;
}
