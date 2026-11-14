package org.freakz.engine.controller;

import org.freakz.common.model.engine.system.SystemSettings;
import org.freakz.common.model.engine.system.WebUiLinksSettings;
import org.freakz.common.model.engine.system.WebUiLinksUpdateRequest;
import org.freakz.engine.services.water.WatchWaterSettingsService;
import org.freakz.engine.services.webui.WebUiLinksSettingsService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/hokan/engine/internal/system")
public class SystemSettingsController {
  private final WatchWaterSettingsService settings;
  private final WebUiLinksSettingsService webUiLinks;
  public SystemSettingsController(WatchWaterSettingsService settings, WebUiLinksSettingsService webUiLinks) {
    this.settings = settings;
    this.webUiLinks = webUiLinks;
  }

  @org.springframework.beans.factory.annotation.Autowired
  private org.freakz.engine.services.water.WatchWaterPoller water;

  @PostMapping(value="/settings/water-refresh", produces="application/json")
  public String refresh(@RequestBody String username) throws Exception { return water.refreshNow(username); }

  @GetMapping("/settings")
  public SystemSettings getSettings() { return settings.getSettings(); }

  @PutMapping("/settings")
  public SystemSettings update(@RequestBody SystemSettings request) {
    try { return settings.update(request); }
    catch (IllegalArgumentException e) { throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage(), e); }
  }

  @GetMapping("/webui-links")
  public WebUiLinksSettings getWebUiLinks() { return webUiLinks.getSettings(); }

  @PutMapping("/webui-links")
  public WebUiLinksSettings updateWebUiLinks(@RequestBody WebUiLinksUpdateRequest request) {
    try { return webUiLinks.update(request); }
    catch (IllegalArgumentException e) { throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage(), e); }
  }
}
