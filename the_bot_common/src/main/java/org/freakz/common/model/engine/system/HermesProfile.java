package org.freakz.common.model.engine.system;

/**
 * A logical Hermes profile that owns its provider configuration directly.
 * Supported providers: {@code openai}, {@code ollama}.
 * <p>
 * Each profile id (chat, coder, ai-command) is the logical route target.
 */
public record HermesProfile(
        /** Unique logical profile id; must match a known route id (chat, coder, ai-command). */
        String id,
        /** Human-readable label shown in the UI. */
        String label,
        /** Provider: openai or ollama. Required. */
        String provider, // openai | ollama
        /** Base URL for the API endpoint. Required when provider=ollama; nullable for openai (env keys used). */
        String baseUrl,
        /** Model name to use. Required for both providers. */
        String model,
        /** API mode: responses or chat-completions. */
        String apiMode,
        /** Timeout in seconds for API calls. */
        Integer timeoutSeconds,
        Boolean healthy,
        Boolean toolCapable,
        String detail,
        /** Context window size (tokens). Meaningful for ollama; nullable for openai. Persisted but not yet passed at runtime. */
        Integer contextWindow,
        Boolean fallbackAllowed,
        String activeProvider,
        Boolean gatewayHealthy,
        Boolean primaryProviderHealthy,
        Boolean fallbackHealthy,
        String cooldownUntil,
        String fallbackReason,
        String fallbackActivatedAt,
        String lastProviderError,
        String lastProviderErrorAt,
        String lastValidatedAt,
        String validationStatus) {

  public HermesProfile(
      String id,
      String label,
      String provider,
      String baseUrl,
      String model,
      String apiMode,
      Integer timeoutSeconds,
      Boolean healthy,
      Boolean toolCapable,
      String detail,
      Integer contextWindow) {
    this(
        id,
        label,
        provider,
        baseUrl,
        model,
        apiMode,
        timeoutSeconds,
        healthy,
        toolCapable,
        detail,
        contextWindow,
        false,
        provider,
        healthy,
        healthy,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null);
  }

  public HermesProfile {
    if (provider == null || provider.isBlank()) {
      throw new IllegalArgumentException("provider is required");
    }
    String prov = provider.trim().toLowerCase();
    if (!"openai".equals(prov) && !"ollama".equals(prov)) {
      throw new IllegalArgumentException("provider must be openai or ollama, got: " + provider);
    }
    // Provider-specific validation
    if ("openai".equals(prov)) {
      if (model == null || model.isBlank()) {
        throw new IllegalArgumentException("provider=openai requires a non-blank model");
      }
    } else if ("ollama".equals(prov)) {
      if (baseUrl == null || baseUrl.isBlank()) {
        throw new IllegalArgumentException("provider=ollama requires a non-blank baseUrl");
      }
      if (model == null || model.isBlank()) {
        throw new IllegalArgumentException("provider=ollama requires a non-blank model");
      }
      if (contextWindow != null && contextWindow <= 0) {
        throw new IllegalArgumentException("provider=ollama contextWindow must be positive when non-null, got: " + contextWindow);
      }
    }
  }
}
