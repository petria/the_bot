package org.freakz.engine.services.ai.hermes;

record HermesSettings(String baseUrl, String apiKey, String model, int timeoutSeconds) {

  boolean configured() {
    return baseUrl != null && !baseUrl.isBlank();
  }
}
