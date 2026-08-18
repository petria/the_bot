package org.freakz.web.controller;

import org.freakz.web.whatsapp.WhatsAppAuthService;
import org.freakz.web.whatsapp.WhatsAppAuthService.WhatsAppAuthResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientResponseException;

@RestController
@RequestMapping("/api/web/admin/whatsapp/auth")
public class AdminWhatsAppAuthController {

  private final WhatsAppAuthService authService;

  public AdminWhatsAppAuthController(WhatsAppAuthService authService) {
    this.authService = authService;
  }

  @GetMapping
  public WhatsAppAuthResponse getStatus() {
    return authService.getStatus();
  }

  @PostMapping("/start")
  @ResponseStatus(HttpStatus.ACCEPTED)
  public WhatsAppAuthResponse start(@RequestBody StartRequest request) {
    return authService.start(request == null ? null : request.method(), request == null ? null : request.phone());
  }

  @PostMapping("/cancel")
  public WhatsAppAuthResponse cancel() {
    return authService.cancel();
  }

  @ExceptionHandler(RestClientResponseException.class)
  public ResponseEntity<ErrorResponse> sidecarError(RestClientResponseException exception) {
    int status = exception.getStatusCode().value() == HttpStatus.UNAUTHORIZED.value()
        || exception.getStatusCode().value() == HttpStatus.FORBIDDEN.value()
        ? HttpStatus.BAD_GATEWAY.value()
        : exception.getStatusCode().value();
    return ResponseEntity.status(status)
        .body(new ErrorResponse(exception.getResponseBodyAsString()));
  }

  @ExceptionHandler(ResourceAccessException.class)
  public ResponseEntity<ErrorResponse> sidecarUnavailable(ResourceAccessException exception) {
    return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
        .body(new ErrorResponse("WhatsApp sidecar is unavailable"));
  }

  public record StartRequest(String method, String phone) {
  }

  public record ErrorResponse(String message) {
  }
}
