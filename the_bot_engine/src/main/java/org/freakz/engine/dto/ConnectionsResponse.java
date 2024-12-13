package org.freakz.engine.dto;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.freakz.common.model.connectionmanager.BotConnectionResponse;
import org.freakz.engine.services.api.ServiceResponse;

import java.util.Map;

@Builder
@Data
@EqualsAndHashCode(callSuper = false)
@ToString
public class ConnectionsResponse extends ServiceResponse {

  private Map<Integer, BotConnectionResponse> connectionMap;
}
