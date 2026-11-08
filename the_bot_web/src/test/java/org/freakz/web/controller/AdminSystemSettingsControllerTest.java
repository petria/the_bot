package org.freakz.web.controller;

import io.micrometer.core.instrument.MeterRegistry;
import org.freakz.common.model.engine.system.SystemSettings;
import org.freakz.common.spring.rest.RestEngineClient;
import org.freakz.common.users.BotPermission;
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
class AdminSystemSettingsControllerTest {
  @Autowired MockMvc mvc;
  @MockitoBean RestEngineClient engine;
  @MockitoBean MeterRegistry registry;
  private static final String URL = "/api/web/admin/system/settings";

  @Test void deniesNonAdminAndRequiresCsrf() throws Exception {
    mvc.perform(get(URL).with(user("user").authorities(() -> BotPermission.WEB_USER))).andExpect(status().isForbidden());
    mvc.perform(put(URL).with(user("user").authorities(() -> BotPermission.WEB_USER)).with(csrf())
        .contentType("application/json").content("{\"waterTemperatureToWatch\":true}")).andExpect(status().isForbidden());
    mvc.perform(put(URL).with(user("admin").authorities(() -> BotPermission.WEB_ADMIN))
        .contentType("application/json").content("{\"waterTemperatureToWatch\":true}")).andExpect(status().isForbidden());
    verify(engine, never()).getSystemSettings();
    verify(engine, never()).updateSystemSettings(any());
  }

  @Test void readsAndSavesForAdminAndRejectsMissingValue() throws Exception {
    when(engine.getSystemSettings()).thenReturn(ResponseEntity.ok(new SystemSettings(false)));
    when(engine.updateSystemSettings(new SystemSettings(true))).thenReturn(ResponseEntity.ok(new SystemSettings(true)));
    mvc.perform(get(URL).with(user("admin").authorities(() -> BotPermission.WEB_ADMIN)))
        .andExpect(status().isOk()).andExpect(jsonPath("$.waterTemperatureToWatch").value(false));
    mvc.perform(put(URL).with(user("admin").authorities(() -> BotPermission.WEB_ADMIN)).with(csrf())
        .contentType("application/json").content("{\"waterTemperatureToWatch\":true}"))
        .andExpect(status().isOk()).andExpect(jsonPath("$.waterTemperatureToWatch").value(true));
    mvc.perform(put(URL).with(user("admin").authorities(() -> BotPermission.WEB_ADMIN)).with(csrf())
        .contentType("application/json").content("{}"))
        .andExpect(status().isBadRequest());
    verify(engine, times(1)).updateSystemSettings(any());
  }
}
