package org.freakz.engine.services.ai.hermes;

record HermesRunResult(String text, boolean timedOut, String error) {

  static HermesRunResult completed(String text) {
    return new HermesRunResult(text, false, null);
  }

  static HermesRunResult timeout() {
    return new HermesRunResult(null, true, null);
  }

  static HermesRunResult failed(String error) {
    return new HermesRunResult(null, false, error == null || error.isBlank() ? "unknown error" : error);
  }
}
