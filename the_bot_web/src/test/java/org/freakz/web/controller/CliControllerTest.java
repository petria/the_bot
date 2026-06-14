package org.freakz.web.controller;

import jakarta.servlet.http.HttpSession;
import org.freakz.common.model.engine.EngineRequest;
import org.freakz.common.model.engine.EngineResponse;
import org.freakz.common.model.users.User;
import org.freakz.common.spring.rest.RestEngineClient;
import org.freakz.web.security.BotUserPrincipal;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CliControllerTest {

  @Test
  void loginCreatesHttpSessionForAuthenticatedUser() {
    AuthenticationManager authenticationManager = mock(AuthenticationManager.class);
    RestEngineClient engineClient = mock(RestEngineClient.class);
    CliController controller = new CliController(authenticationManager, engineClient);
    BotUserPrincipal principal = principal("petria");
    when(authenticationManager.authenticate(any()))
        .thenReturn(UsernamePasswordAuthenticationToken.authenticated(
            principal,
            null,
            principal.getAuthorities()));

    MockHttpServletRequest request = new MockHttpServletRequest();
    MockHttpServletResponse response = new MockHttpServletResponse();

    CliController.LoginResponse login = controller.login(
        new CliController.LoginRequest("petria", "secret"),
        request,
        response);

    assertThat(login.username()).isEqualTo("petria");
    HttpSession session = request.getSession(false);
    assertThat(session).isNotNull();
    assertThat(session.getAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY)).isNotNull();
  }

  @Test
  void loginRejectsInvalidCredentials() {
    AuthenticationManager authenticationManager = mock(AuthenticationManager.class);
    RestEngineClient engineClient = mock(RestEngineClient.class);
    CliController controller = new CliController(authenticationManager, engineClient);
    when(authenticationManager.authenticate(any())).thenThrow(new BadCredentialsException("bad"));

    assertThatThrownBy(() -> controller.login(
        new CliController.LoginRequest("petria", "wrong"),
        new MockHttpServletRequest(),
        new MockHttpServletResponse()))
        .isInstanceOf(ResponseStatusException.class)
        .extracting("statusCode.value")
        .isEqualTo(401);
  }

  @Test
  void commandUsesLoggedInUsernameAsSender() {
    AuthenticationManager authenticationManager = mock(AuthenticationManager.class);
    RestEngineClient engineClient = mock(RestEngineClient.class);
    CliController controller = new CliController(authenticationManager, engineClient);
    EngineResponse engineResponse = EngineResponse.builder().message("pong").build();
    when(engineClient.handleEngineRequest(any())).thenReturn(ResponseEntity.ok(engineResponse));
    BotUserPrincipal principal = principal("petria");

    CliController.CommandResponse response = controller.command(
        principal,
        new CliController.CommandRequest("!ping"));

    assertThat(response.username()).isEqualTo("petria");
    assertThat(response.reply()).isEqualTo("pong");

    ArgumentCaptor<EngineRequest> captor = ArgumentCaptor.forClass(EngineRequest.class);
    verify(engineClient).handleEngineRequest(captor.capture());
    EngineRequest request = captor.getValue();
    assertThat(request.getFromSender()).isEqualTo("petria");
    assertThat(request.getNetwork()).isEqualTo("BOT_CLI_CLIENT");
    assertThat(request.getEchoToAlias()).isEqualTo("THE_BOT_CLI");
    assertThat(request.getCommand()).isEqualTo("!ping");
  }

  private BotUserPrincipal principal(String username) {
    User user = User.builder()
        .username(username)
        .password("$2a$10$abcdefghijklmnopqrstuv")
        .permissions(List.of("web_user"))
        .build();
    user.setId(1L);
    return BotUserPrincipal.from(user, List.of(new SimpleGrantedAuthority("web_user")));
  }
}
