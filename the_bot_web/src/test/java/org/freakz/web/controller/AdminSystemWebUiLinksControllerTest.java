package org.freakz.web.controller;

import io.micrometer.core.instrument.MeterRegistry;
import org.freakz.common.model.engine.system.WebUiLinksSettings;
import org.freakz.common.model.engine.system.WebUiLinksUpdateRequest;
import org.freakz.common.users.BotPermission;
import org.freakz.common.spring.rest.RestEngineClient;
import org.freakz.web.security.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AdminSystemController.class)
@Import(SecurityConfig.class)
class AdminSystemWebUiLinksControllerTest {
  @Autowired MockMvc mvc;
  @MockitoBean RestEngineClient engine;
  @MockitoBean MeterRegistry registry;
  private static final String URL = "/api/web/admin/system/webui-links";

  private static WebUiLinksSettings links(boolean configured) {
    return new WebUiLinksSettings(
        new WebUiLinksSettings.WebUiLink("https://hokanthebot.airiot.fi", configured),
        new WebUiLinksSettings.WebUiLink("https://hokandevbot.airiot.fi", configured));
  }

  @Test void deniesNonAdminAndRequiresCsrf() throws Exception {
    mvc.perform(get(URL).with(user("user").authorities(() -> BotPermission.WEB_USER))).andExpect(status().isForbidden());
    mvc.perform(put(URL).with(user("admin").authorities(() -> BotPermission.WEB_ADMIN))
        .contentType("application/json").content("{\"mainUrl\":\"https://x.example\"}")).andExpect(status().isForbidden());
    verify(engine, never()).getWebUiLinks();
    verify(engine, never()).updateWebUiLinks(any());
  }

  @Test void readsAndSavesForAdmin() throws Exception {
    when(engine.getWebUiLinks()).thenReturn(ResponseEntity.ok(links(false)));
    when(engine.updateWebUiLinks(new WebUiLinksUpdateRequest("https://custom.example", "")))
        .thenReturn(ResponseEntity.ok(links(true)));
    mvc.perform(get(URL).with(user("admin").authorities(() -> BotPermission.WEB_ADMIN)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.main.url").value("https://hokanthebot.airiot.fi"))
        .andExpect(jsonPath("$.main.configured").value(false));
    mvc.perform(put(URL).with(user("admin").authorities(() -> BotPermission.WEB_ADMIN)).with(csrf())
        .contentType("application/json").content("{\"mainUrl\":\"https://custom.example\",\"developUrl\":\"\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.main.configured").value(true));
    verify(engine, times(1)).updateWebUiLinks(new WebUiLinksUpdateRequest("https://custom.example", ""));
  }

  @Test void engineRejectionBecomesBadRequestWithReason() throws Exception {
    when(engine.updateWebUiLinks(any())).thenReturn(ResponseEntity.badRequest()
        .body(new WebUiLinksSettings(new WebUiLinksSettings.WebUiLink("", false),
            new WebUiLinksSettings.WebUiLink("", false))));
    mvc.perform(put(URL).with(user("admin").authorities(() -> BotPermission.WEB_ADMIN)).with(csrf())
        .contentType("application/json").content("{\"mainUrl\":\"javascript:alert(1)\",\"developUrl\":\"\"}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message").value("Could not save Web UI links in bot-engine"));
  }
}
