package org.freakz.hermesmanager.service;

public class HermesValidationException extends RuntimeException {

  private final String detail;

  public HermesValidationException(String message, String detail, Throwable cause) {
    super(message, cause);
    this.detail = detail;
  }

  public String getDetail() {
    return detail;
  }
}
