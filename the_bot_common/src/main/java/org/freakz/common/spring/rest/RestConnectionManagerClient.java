package org.freakz.common.spring.rest;

import org.freakz.common.model.connectionmanager.ChannelUsersByEchoToAliasRequest;
import org.freakz.common.model.connectionmanager.ChannelUsersByEchoToAliasResponse;
import org.freakz.common.model.connectionmanager.GetConnectionMapResponse;
import org.freakz.common.model.connectionmanager.GetChannelActivityResponse;
import org.freakz.common.model.connectionmanager.GetKnownChatChannelsResponse;
import org.freakz.common.model.connectionmanager.GetKnownChatUsersResponse;
import org.freakz.common.model.connectionmanager.GetKnownUserTargetsResponse;
import org.freakz.common.model.connectionmanager.IrcOperatorReconcileRequest;
import org.freakz.common.model.connectionmanager.IrcOperatorReconcileResponse;
import org.freakz.common.model.connectionmanager.IrcOperatorStateResponse;
import org.freakz.common.model.connectionmanager.IrcOperatorGrantRequest;
import org.freakz.common.model.connectionmanager.IrcOperatorGrantResponse;
import org.freakz.common.model.connectionmanager.IrcOperatorModeRequest;
import org.freakz.common.model.connectionmanager.IrcOperatorModeResponse;
import org.freakz.common.model.connectionmanager.IrcChannelControlRequest;
import org.freakz.common.model.connectionmanager.IrcChannelControlResponse;
import org.freakz.common.model.connectionmanager.IrcTopicEventResponse;
import org.freakz.common.model.connectionmanager.IrcTopicEventRequest;
import org.freakz.common.model.connectionmanager.IrcTopicSetRequest;
import org.freakz.common.model.connectionmanager.IrcTopicSetResponse;
import org.freakz.common.model.connectionmanager.IrcTopicStateResponse;
import org.freakz.common.model.connectionmanager.IrcTopicStatesResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.client.RestTemplate;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Component
public class RestConnectionManagerClient {

  private static final Logger log = LoggerFactory.getLogger(RestConnectionManagerClient.class);
  private final RestTemplate restTemplate;
  private final String baseUrl;

  @Autowired
  public RestConnectionManagerClient(
      RestTemplate restTemplate,
      @Value("${the.bot.rest.bot-io-base-url:http://bot-io:8090}") String botIoBaseUrl,
      @Value("${the.bot.internal-api-token:}") String internalApiToken) {
    this.restTemplate = InternalRestTemplate.withToken(restTemplate, internalApiToken);
    this.baseUrl = trimTrailingSlash(botIoBaseUrl) + "/api/hokan/io/connection_manager";
  }

  public GetConnectionMapResponse getConnectionMap() {
    try {
      return getConnectionMapRequired();
    } catch (Exception e) {
      log.error("Error sending getConnectionMap message: {}", e.getMessage());
      return new GetConnectionMapResponse();
    }

  }

  public GetConnectionMapResponse getConnectionMapRequired() {
    return restTemplate.getForObject(baseUrl + "/get_connection_map", GetConnectionMapResponse.class);
  }

  public ResponseEntity<ChannelUsersByEchoToAliasResponse> getChannelUsersByEchoToAlias(@RequestBody ChannelUsersByEchoToAliasRequest request) {
    String url = baseUrl + "/get_channel_users_by_echo_to_alias";
    try {
      return restTemplate.postForEntity(url, request, ChannelUsersByEchoToAliasResponse.class);
    } catch (Exception e) {
      log.error("Error sending getChannelUsersByEchoToAlias message: {}", e.getMessage());
      return ResponseEntity.internalServerError().body(new ChannelUsersByEchoToAliasResponse());
    }
  }

  public GetChannelActivityResponse getChannelActivity() {
    try {
      return getChannelActivityRequired();
    } catch (Exception e) {
      log.error("Error sending getChannelActivity message: {}", e.getMessage());
      return new GetChannelActivityResponse();
    }
  }

  public GetChannelActivityResponse getChannelActivityRequired() {
    return restTemplate.getForObject(baseUrl + "/get_channel_activity", GetChannelActivityResponse.class);
  }

  public GetKnownChatChannelsResponse getKnownChannels() {
    String url = baseUrl + "/get_known_channels";
    try {
      return restTemplate.getForObject(url, GetKnownChatChannelsResponse.class);
    } catch (Exception e) {
      log.error("Error sending getKnownChannels message: {}", e.getMessage());
      return new GetKnownChatChannelsResponse();
    }
  }

  public GetKnownChatUsersResponse getKnownUsers(String query) {
    String url = withOptionalQuery(baseUrl + "/get_known_users", query);
    try {
      return restTemplate.getForObject(url, GetKnownChatUsersResponse.class);
    } catch (Exception e) {
      log.error("Error sending getKnownUsers message: {}", e.getMessage());
      return new GetKnownChatUsersResponse();
    }
  }

  public GetKnownUserTargetsResponse getKnownUserTargets(String query) {
    try {
      return getKnownUserTargetsRequired(query);
    } catch (Exception e) {
      log.error("Error sending getKnownUserTargets message: {}", e.getMessage());
      return new GetKnownUserTargetsResponse();
    }
  }

  public GetKnownUserTargetsResponse getKnownUserTargetsRequired(String query) {
    String url = withOptionalQuery(baseUrl + "/get_known_user_targets", query);
    return restTemplate.getForObject(url, GetKnownUserTargetsResponse.class);
  }

  public IrcOperatorStateResponse getIrcOperatorState(String echoToAlias) {
    String url = baseUrl + "/irc/operator_state?echoToAlias="
        + URLEncoder.encode(echoToAlias, StandardCharsets.UTF_8);
    return restTemplate.getForObject(url, IrcOperatorStateResponse.class);
  }

  public IrcOperatorReconcileResponse reconcileIrcOperators(IrcOperatorReconcileRequest request) {
    return restTemplate.postForObject(
        baseUrl + "/irc/operators/reconcile",
        request,
        IrcOperatorReconcileResponse.class);
  }

  public IrcOperatorGrantResponse grantIrcOperator(IrcOperatorGrantRequest request) {
    return restTemplate.postForObject(
        baseUrl + "/irc/operators/grant",
        request,
        IrcOperatorGrantResponse.class);
  }

  public IrcOperatorModeResponse setIrcOperatorMode(IrcOperatorModeRequest request) {
    return restTemplate.postForObject(
        baseUrl + "/irc/operators/mode",
        request,
        IrcOperatorModeResponse.class);
  }

  public IrcChannelControlResponse controlIrcChannel(IrcChannelControlRequest request) {
    return restTemplate.postForObject(
        baseUrl + "/irc/channel_control",
        request,
        IrcChannelControlResponse.class);
  }

  public IrcTopicSetResponse setIrcTopic(IrcTopicSetRequest request) {
    return restTemplate.postForObject(baseUrl + "/irc/topic", request, IrcTopicSetResponse.class);
  }

  public List<IrcTopicStateResponse> getIrcTopicStates() {
    try {
      IrcTopicStatesResponse response = restTemplate.getForObject(
          baseUrl + "/irc/topic_states", IrcTopicStatesResponse.class);
      return response == null ? List.of() : response.topics();
    } catch (Exception e) {
      log.debug("Error reading IRC topic states: {}", e.getMessage());
      return List.of();
    }
  }

  public IrcTopicEventResponse handleIrcTopicEvent(IrcTopicEventRequest request) {
    return restTemplate.postForObject(baseUrl + "/irc/topic_event", request, IrcTopicEventResponse.class);
  }

  private String withOptionalQuery(String url, String query) {
    if (query == null || query.isBlank()) {
      return url;
    }
    return url + "?query=" + URLEncoder.encode(query, StandardCharsets.UTF_8);
  }

  private String trimTrailingSlash(String value) {
    return value == null ? "" : value.replaceFirst("/+$", "");
  }
}
