package org.freakz.io.contoller;

import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.freakz.common.exception.InvalidTargetAliasException;
import org.freakz.common.model.connectionmanager.ChannelUser;
import org.freakz.common.model.connectionmanager.ChannelUsersByTargetAliasRequest;
import org.freakz.common.model.connectionmanager.ChannelUsersByTargetAliasResponse;
import org.freakz.common.model.connectionmanager.GetConnectionMapResponse;
import org.freakz.io.connections.BotConnection;
import org.freakz.io.connections.ConnectionManager;
import org.freakz.io.connections.JoinedChannelContainer;
import org.freakz.io.mappers.DataToDTOMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/hokan/io/connection_manager")
@Slf4j
public class ConnectionManagerController {

  private final ConnectionManager connectionManager;
  private final DataToDTOMapper mapper;

  @Autowired
  public ConnectionManagerController(ConnectionManager connectionManager, DataToDTOMapper mapper) {
    this.connectionManager = connectionManager;
    this.mapper = mapper;
  }

  @GetMapping("/get_connection_map")
  public ResponseEntity<?> getConnectionMap() {
    Map<Integer, BotConnection> connectionMap = connectionManager.getConnectionMap();
    GetConnectionMapResponse response = mapper.toGetConnectionMapResponse(connectionMap);
    return ResponseEntity.ok(response);
  }

  @GetMapping("/get_joined_channels_map")
  public ResponseEntity<?> getJoinedChannelsMap() {
    //        Map<Integer, BotConnection> connectionMap = connectionManager.getConnectionMap();
    Map<String, JoinedChannelContainer> joinedChannelsMap =
        connectionManager.getJoinedChannelsMap();
    //        GetConnectionMapResponse response = mapper.toGetConnectionMapResponse(connectionMap);
    return ResponseEntity.ok(joinedChannelsMap);
  }

  @PostMapping("/get_channel_users_by_target_alias")
  public ResponseEntity<?> getChannelUsersByTargetAlias(
      @RequestBody ChannelUsersByTargetAliasRequest request) throws InvalidTargetAliasException {
    ChannelUsersByTargetAliasResponse response = new ChannelUsersByTargetAliasResponse();

    List<ChannelUser> users =
        connectionManager.getChannelUsersByTargetAlias(request.getTargetAlias());
    response.setChannelUsers(users);
    //        response.setChannelUsers(List.of("Fuu", "Bar", "Test"));
    return ResponseEntity.ok(response);
  }
}
