package org.freakz.io.contoller;

import org.freakz.common.exception.InvalidEchoToAliasException;
import org.freakz.common.model.connectionmanager.ChannelUser;
import org.freakz.common.model.connectionmanager.GetChannelActivityResponse;
import org.freakz.common.model.connectionmanager.ChannelUsersByEchoToAliasRequest;
import org.freakz.common.model.connectionmanager.ChannelUsersByEchoToAliasResponse;
import org.freakz.common.model.connectionmanager.GetKnownChatChannelsResponse;
import org.freakz.common.model.connectionmanager.GetKnownChatUsersResponse;
import org.freakz.common.model.connectionmanager.GetKnownUserTargetsResponse;
import org.freakz.common.model.connectionmanager.GetConnectionMapResponse;
import org.freakz.io.connections.BotConnection;
import org.freakz.io.connections.ConnectionManager;
import org.freakz.io.connections.JoinedChannelContainer;
import org.freakz.io.mappers.DataToDTOMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/hokan/io/connection_manager")
public class ConnectionManagerController {

  private static final Logger log = LoggerFactory.getLogger(ConnectionManagerController.class);

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

  @GetMapping("/get_channel_activity")
  public ResponseEntity<?> getChannelActivity() {
    GetChannelActivityResponse response = GetChannelActivityResponse.builder()
        .channels(connectionManager.getChannelActivity())
        .build();
    return ResponseEntity.ok(response);
  }

  @GetMapping("/get_known_channels")
  public ResponseEntity<?> getKnownChannels() {
    return ResponseEntity.ok(new GetKnownChatChannelsResponse(connectionManager.getKnownChannels()));
  }

  @GetMapping("/get_known_users")
  public ResponseEntity<?> getKnownUsers(@RequestParam(required = false) String query) {
    if (query == null || query.isBlank()) {
      return ResponseEntity.ok(new GetKnownChatUsersResponse(connectionManager.getKnownUsers()));
    }
    return ResponseEntity.ok(new GetKnownChatUsersResponse(connectionManager.findKnownUsers(query)));
  }

  @GetMapping("/get_known_user_targets")
  public ResponseEntity<?> getKnownUserTargets(@RequestParam(required = false) String query) {
    if (query == null || query.isBlank()) {
      return ResponseEntity.ok(new GetKnownUserTargetsResponse(connectionManager.getKnownUserTargets()));
    }
    return ResponseEntity.ok(new GetKnownUserTargetsResponse(connectionManager.findKnownUserTargets(query)));
  }

  @PostMapping("/get_channel_users_by_echo_to_alias")
  public ResponseEntity<?> getChannelUsersByEchoToAlias(
      @RequestBody ChannelUsersByEchoToAliasRequest request) throws InvalidEchoToAliasException {
    ChannelUsersByEchoToAliasResponse response = new ChannelUsersByEchoToAliasResponse();

    List<ChannelUser> users =
        connectionManager.getChannelUsersByEchoToAlias(request.getEchoToAlias());
    response.setChannelUsers(users);
    //        response.setChannelUsers(List.of("Fuu", "Bar", "Test"));
    return ResponseEntity.ok(response);
  }
}
