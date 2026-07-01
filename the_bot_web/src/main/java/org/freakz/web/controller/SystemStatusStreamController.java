package org.freakz.web.controller;

import org.freakz.common.model.system.SystemStatusResponse;
import org.freakz.web.system.SystemStatusStreamService;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/web/system/status")
public class SystemStatusStreamController {

  private final SystemStatusStreamService streamService;

  public SystemStatusStreamController(SystemStatusStreamService streamService) {
    this.streamService = streamService;
  }

  @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
  public ResponseEntity<SseEmitter> stream() {
    return sseResponse(streamService.subscribe());
  }

  @PostMapping("/refresh")
  public SystemStatusResponse refresh() {
    return streamService.refreshNow();
  }

  private ResponseEntity<SseEmitter> sseResponse(SseEmitter emitter) {
    return ResponseEntity.ok()
        .cacheControl(CacheControl.noCache())
        .header(HttpHeaders.CONNECTION, "keep-alive")
        .header("X-Accel-Buffering", "no")
        .contentType(MediaType.TEXT_EVENT_STREAM)
        .body(emitter);
  }
}
