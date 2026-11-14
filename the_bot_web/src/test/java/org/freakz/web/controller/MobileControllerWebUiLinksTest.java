package org.freakz.web.controller;

import org.freakz.common.model.engine.system.WebUiLinksSettings;
import org.freakz.common.spring.rest.RestEngineClient;
import org.freakz.web.mobile.MobileAuthService;
import org.freakz.web.mobile.MobileNotificationService;
import org.freakz.web.security.BotUserPrincipal;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class MobileControllerWebUiLinksTest {

  private final RestEngineClient engine = mock(RestEngineClient.class);
  private final MobileController controller = new MobileController(
      mock(MobileAuthService.class), mock(MobileNotificationService.class), engine,
      mock(ConnectionsController.class));

  private final BotUserPrincipal principal = new BotUserPrincipal(
      1L, "tester", null, "Tester", null, null, null, null, null, List.of("web.user"), List.of());

  @Test void returnsEngineLinksForAuthenticatedUser() {
    WebUiLinksSettings links = new WebUiLinksSettings(
        new WebUiLinksSettings.WebUiLink("https://hokanthebot.airiot.fi", false),
        new WebUiLinksSettings.WebUiLink("https://custom.example", true));
    when(engine.getWebUiLinks()).thenReturn(ResponseEntity.ok(links));

    WebUiLinksSettings result = controller.webUiLinks(principal);

    assertThat(result.main().url()).isEqualTo("https://hokanthebot.airiot.fi");
    assertThat(result.develop().configured()).isTrue();
    verify(engine).getWebUiLinks();
  }

  @Test void engineFailureSurfacesAsBadGateway() {
    when(engine.getWebUiLinks()).thenReturn(ResponseEntity.status(500).build());
    assertThatThrownBy(() -> controller.webUiLinks(principal))
        .isInstanceOfSatisfying(ResponseStatusException.class,
            e -> assertThat(e.getStatusCode().value()).isEqualTo(502));
  }
}
