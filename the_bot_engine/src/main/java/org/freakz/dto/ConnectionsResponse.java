package org.freakz.dto;

import lombok.Builder;
import lombok.Data;
import org.freakz.common.dto.BotConnection;
import org.freakz.services.ServiceResponse;

import java.util.Map;

@Builder
@Data
public class ConnectionsResponse extends ServiceResponse {

    private Map<Integer, BotConnection> connectionMap;
}
