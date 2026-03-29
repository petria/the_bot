package org.freakz.common.spring.rest;

import org.freakz.common.model.connectionmanager.ChannelUsersByEchoToAliasRequest;
import org.freakz.common.model.connectionmanager.ChannelUsersByEchoToAliasResponse;
import org.freakz.common.model.connectionmanager.GetConnectionMapResponse;
import org.freakz.common.model.connectionmanager.GetChannelActivityResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.client.RestTemplate;

@Component
public class RestConnectionManagerClient {

  private static final Logger log = LoggerFactory.getLogger(RestConnectionManagerClient.class);
  private final RestTemplate restTemplate;
  private final String BASE_URL = "http://bot-io:8090/api/hokan/io/connection_manager";

  @Autowired
  public RestConnectionManagerClient(RestTemplate restTemplate) {
    this.restTemplate = restTemplate;
  }

  public GetConnectionMapResponse getConnectionMap() {
    String url = BASE_URL + "/get_connection_map";
    try {
      return restTemplate.getForObject(url, GetConnectionMapResponse.class);
    } catch (Exception e) {
      log.error("Error sending getConnectionMap message: {}", e.getMessage());
      return new GetConnectionMapResponse();
    }

  }

  public ResponseEntity<ChannelUsersByEchoToAliasResponse> getChannelUsersByEchoToAlias(@RequestBody ChannelUsersByEchoToAliasRequest request) {
    String url = BASE_URL + "/get_channel_users_by_echo_to_alias";
    try {
      return restTemplate.postForEntity(url, request, ChannelUsersByEchoToAliasResponse.class);
    } catch (Exception e) {
      log.error("Error sending getChannelUsersByEchoToAlias message: {}", e.getMessage());
      return ResponseEntity.internalServerError().body(new ChannelUsersByEchoToAliasResponse());
    }
  }

  public GetChannelActivityResponse getChannelActivity() {
    String url = BASE_URL + "/get_channel_activity";
    try {
      return restTemplate.getForObject(url, GetChannelActivityResponse.class);
    } catch (Exception e) {
      log.error("Error sending getChannelActivity message: {}", e.getMessage());
      return new GetChannelActivityResponse();
    }
  }

}
