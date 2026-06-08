package org.freakz.hermesmanager.controller;

import org.freakz.common.model.engine.system.HermesFallbackModelsResponse;
import org.freakz.common.model.engine.system.HermesFallbackSettingsResponse;
import org.freakz.common.model.engine.system.HermesFallbackUpdateRequest;
import org.freakz.hermesmanager.service.HermesFallbackService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/hermes/fallback")
public class HermesFallbackController {

  private final HermesFallbackService service;

  public HermesFallbackController(HermesFallbackService service) {
    this.service = service;
  }

  @GetMapping
  public HermesFallbackSettingsResponse getSettings() {
    return service.getSettings();
  }

  @GetMapping("/models")
  public HermesFallbackModelsResponse getModels(@RequestParam String baseUrl) {
    return service.getModels(baseUrl);
  }

  @PutMapping
  public HermesFallbackSettingsResponse update(@RequestBody HermesFallbackUpdateRequest request) {
    return service.update(request);
  }
}
