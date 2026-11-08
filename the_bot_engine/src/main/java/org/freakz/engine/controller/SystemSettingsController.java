package org.freakz.engine.controller;

import org.freakz.common.model.engine.system.SystemSettings;
import org.freakz.engine.services.water.WatchWaterSettingsService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/hokan/engine/internal/system/settings")
public class SystemSettingsController {
  private final WatchWaterSettingsService settings;
  public SystemSettingsController(WatchWaterSettingsService settings) { this.settings = settings; }

  @org.springframework.beans.factory.annotation.Autowired
  private org.freakz.engine.services.water.WatchWaterPoller water;

  @PostMapping(value="/water-refresh", produces="application/json")
  public String refresh(@RequestBody String username) throws Exception { return water.refreshNow(username); }

  @GetMapping
  public SystemSettings getSettings() { return settings.getSettings(); }

  @PutMapping
  public SystemSettings update(@RequestBody SystemSettings request) {
    try { return settings.update(request); }
    catch (IllegalArgumentException e) { throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage(), e); }
  }
}
