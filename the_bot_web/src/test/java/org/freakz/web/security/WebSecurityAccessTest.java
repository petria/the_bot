package org.freakz.web.security;

import io.micrometer.core.instrument.MeterRegistry;
import org.freakz.common.users.BotPermission;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.ComponentScan.Filter;
import org.springframework.context.annotation.Import;
import org.springframework.stereotype.Controller;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
  }

  @Test
  void webUserCanUseNormalApiButNotAdminApi() throws Exception {
    mockMvc.perform(get("/api/web/test").with(user("web-user").authorities(() -> BotPermission.WEB_USER)))
        .andExpect(status().isOk());
    mockMvc.perform(get("/api/web/admin/test").with(user("web-user").authorities(() -> BotPermission.WEB_USER)))
        .andExpect(status().isForbidden());
  }

  @Test
  void webAdminCanUseNormalAndAdminApis() throws Exception {
    mockMvc.perform(get("/api/web/test").with(user("web-admin").authorities(() -> BotPermission.WEB_ADMIN)))
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

    @GetMapping("/api/web/test")
    String normalApi() {
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
  }
}
