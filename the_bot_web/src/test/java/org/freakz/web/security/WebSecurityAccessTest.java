package org.freakz.web.security;

import io.micrometer.core.instrument.MeterRegistry;
import org.freakz.common.users.BotPermission;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.ComponentScan.Filter;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import tools.jackson.databind.json.JsonMapper;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
    useDefaultFilters = false,
    includeFilters = @Filter(Controller.class),
    controllers = WebSecurityAccessTest.TestController.class
)
@Import(SecurityConfig.class)
class WebSecurityAccessTest {

  @Autowired
  private MockMvc mockMvc;

  @MockitoBean
  private MeterRegistry meterRegistry;

  @Test
  void publicGeneratedPagesDoNotRequireLogin() throws Exception {
    mockMvc.perform(get("/generated/test")).andExpect(status().isOk());
    mockMvc.perform(get("/api/web/generated-pages/test")).andExpect(status().isOk());
    mockMvc.perform(get("/m/test")).andExpect(status().isOk());
  }

  @Test
  void cliLoginRequiresCsrfAndAllowsAnonymousTokenBootstrap() throws Exception {
    var bootstrap = mockMvc.perform(get("/api/web/cli/csrf"))
        .andExpect(status().isOk())
        .andReturn();
    var token = new JsonMapper().readTree(bootstrap.getResponse().getContentAsByteArray());
    mockMvc.perform(post("/api/web/cli/login")).andExpect(status().isForbidden());
    var login = post("/api/web/cli/login").header(token.get("headerName").asText(), token.get("token").asText());
    var cookie = bootstrap.getResponse().getCookie("XSRF-TOKEN");
    if (cookie != null) {
      login.cookie(cookie);
    } else {
      login.session((MockHttpSession) bootstrap.getRequest().getSession(false));
    }
    mockMvc.perform(login).andExpect(status().isBadRequest());
  }

  @Test
  void webUserCanUseNormalApiButNotAdminApi() throws Exception {
    mockMvc.perform(get("/api/web/test").with(user("web-user").authorities(() -> BotPermission.WEB_USER)))
        .andExpect(status().isOk());
    mockMvc.perform(get("/api/web/live-channels/test").with(user("web-user").authorities(() -> BotPermission.WEB_USER)))
        .andExpect(status().isOk());
    mockMvc.perform(get("/api/web/admin/test").with(user("web-user").authorities(() -> BotPermission.WEB_USER)))
        .andExpect(status().isForbidden());
    mockMvc.perform(post("/api/web/messages/test")
            .with(user("web-user").authorities(() -> BotPermission.WEB_USER))
            .with(csrf()))
        .andExpect(status().isForbidden());
  }

  @Test
  void webAdminCanUseNormalAndAdminApis() throws Exception {
    mockMvc.perform(get("/api/web/test").with(user("web-admin").authorities(() -> BotPermission.WEB_ADMIN)))
        .andExpect(status().isOk());
    mockMvc.perform(post("/api/web/messages/test")
            .with(user("web-admin").authorities(() -> BotPermission.WEB_ADMIN))
            .with(csrf()))
        .andExpect(status().isOk());
    mockMvc.perform(get("/api/web/admin/test").with(user("web-admin").authorities(() -> BotPermission.WEB_ADMIN)))
        .andExpect(status().isOk());
  }

  @Test
  void authenticatedUserWithoutWebPermissionCanUseProfileApiOnly() throws Exception {
    mockMvc.perform(get("/api/web/test").with(user("profile-user").authorities(() -> "ROLE_USER")))
        .andExpect(status().isForbidden());
    mockMvc.perform(get("/api/web/me/test").with(user("profile-user").authorities(() -> "ROLE_USER")))
        .andExpect(status().isOk());
  }

  @RestController
  static class TestController {

    @GetMapping("/generated/test")
    String generatedPage() {
      return "ok";
    }

    @GetMapping("/api/web/generated-pages/test")
    String generatedApi() {
      return "ok";
    }

    @GetMapping("/m/test")
    String shortMedia() {
      return "ok";
    }

    @GetMapping("/api/web/test")
    String normalApi() {
      return "ok";
    }

    @GetMapping("/api/web/live-channels/test")
    String liveChannelsApi() {
      return "ok";
    }

    @GetMapping("/api/web/me/test")
    String meApi() {
      return "ok";
    }

    @GetMapping("/api/web/admin/test")
    String adminApi() {
      return "ok";
    }

    @PostMapping("/api/web/messages/test")
    String messageApi() {
      return "ok";
    }
  }
}
