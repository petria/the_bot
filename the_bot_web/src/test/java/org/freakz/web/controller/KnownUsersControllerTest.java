package org.freakz.web.controller;

import org.freakz.common.model.connectionmanager.GetKnownUserTargetsResponse;
import org.freakz.common.model.connectionmanager.KnownUserTargetResponse;
import org.freakz.common.model.users.User;
import org.freakz.common.spring.rest.RestConnectionManagerClient;
import org.freakz.web.channels.ChannelAccessService;
import org.freakz.web.config.TheBotWebProperties;
import org.freakz.web.security.BotUserPrincipal;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class KnownUsersControllerTest {

  @Test
  void targetsAreFilteredToVisiblePublicChannels() {
    RestConnectionManagerClient connectionManagerClient = mock(RestConnectionManagerClient.class);
    KnownUsersController controller = new KnownUsersController(
        connectionManagerClient,
        new TheBotWebProperties(),
        new ChannelAccessService());
    when(connectionManagerClient.getKnownUserTargetsRequired(null)).thenReturn(new GetKnownUserTargetsResponse(List.of(
        target("IRC_CONNECTION", "IRC-AMIGAFIN", "PUBLIC"),
        target("DISCORD_CONNECTION", "DISCORD-HOKANDEV", "PUBLIC"),
        target("IRC_CONNECTION", "PRIVATE-IRC-foo", "PRIVATE"))));

    ResponseEntity<?> entity = controller.getTargets(principal("channels.view.irc"), null);

    GetKnownUserTargetsResponse body = (GetKnownUserTargetsResponse) entity.getBody();
    assertThat(body).isNotNull();
    assertThat(body.getTargets())
        .extracting(KnownUserTargetResponse::getEchoToAlias)
        .containsExactly("IRC-AMIGAFIN");
  }

  private KnownUserTargetResponse target(String connectionType, String echoToAlias, String targetType) {
    return new KnownUserTargetResponse(
        "logical",
        null,
        null,
        null,
        false,
        "OBSERVED_ONLY",
        "observed",
        "observed-id",
        "observed-user",
        "Observed User",
        1,
        connectionType,
        "network",
        "channel-id",
        "channel-name",
        echoToAlias,
        targetType,
        1L,
        "source");
  }

  private BotUserPrincipal principal(String... permissions) {
    User user = User.builder()
        .username("test")
        .password("hash")
        .permissions(List.of(permissions))
        .build();
    return BotUserPrincipal.from(user, List.of(permissions).stream()
        .map(SimpleGrantedAuthority::new)
        .map(GrantedAuthority.class::cast)
        .toList());
  }
}
