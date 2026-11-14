package org.freakz.common.model.engine.system;

/** Bot-controlled Web UI links offered to clients such as the Android app. */
public record WebUiLinksSettings(WebUiLink main, WebUiLink develop) {

  /** A link together with whether the URL came from an operator override instead of a default. */
  public record WebUiLink(String url, boolean configured) {
  }
}
