package org.freakz.common.spring.rest;

import lombok.extern.slf4j.Slf4j;
import org.freakz.common.model.connectionmanager.ChannelUsersByTargetAliasRequest;
import org.freakz.common.model.connectionmanager.ChannelUsersByTargetAliasResponse;
import org.freakz.common.model.connectionmanager.GetConnectionMapResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.client.RestTemplate;

@Component
@Slf4j
public class RestConnectionManagerClient {

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

  public ResponseEntity<ChannelUsersByTargetAliasResponse> getChannelUsersByTargetAlias(@RequestBody ChannelUsersByTargetAliasRequest request) {
    String url = BASE_URL + "/get_channel_users_by_target_alias";
    try {
      return restTemplate.postForEntity(url, request, ChannelUsersByTargetAliasResponse.class);
    } catch (Exception e) {
      log.error("Error sending getChannelUsersByTargetAlias message: {}", e.getMessage());
      return ResponseEntity.internalServerError().body(new ChannelUsersByTargetAliasResponse());
    }
  }

}
