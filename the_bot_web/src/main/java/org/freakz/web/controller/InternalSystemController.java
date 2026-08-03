package org.freakz.web.controller;

import org.freakz.common.model.system.SystemStatusResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/system")
public class InternalSystemController {

  private final SystemController systemController;

  public InternalSystemController(SystemController systemController) {
    this.systemController = systemController;
  }

  @GetMapping("/status")
  public SystemStatusResponse getStatus() {
    return systemController.getStatus();
  }
}
