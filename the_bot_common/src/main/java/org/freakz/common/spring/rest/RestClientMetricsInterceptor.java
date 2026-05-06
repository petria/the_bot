package org.freakz.common.spring.rest;

import java.io.IOException;
import java.net.URI;

import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;

public class RestClientMetricsInterceptor implements ClientHttpRequestInterceptor {

  private final MeterRegistry meterRegistry;
  private final Environment environment;

  public RestClientMetricsInterceptor(MeterRegistry meterRegistry, Environment environment) {
    this.meterRegistry = meterRegistry;
    this.environment = environment;
  }

  @Override
  public ClientHttpResponse intercept(
      HttpRequest request,
      byte[] body,
      ClientHttpRequestExecution execution) throws IOException {
    try {
      ClientHttpResponse response = execution.execute(request, body);
      record(request, target(request.getURI()), Integer.toString(response.getStatusCode().value()));
      return response;
    } catch (IOException | RuntimeException e) {
      record(request, target(request.getURI()), "exception");
      throw e;
    }
  }

  private void record(HttpRequest request, String target, String outcome) {
    meterRegistry.counter(
        "thebot.http.client.requests",
        "source", source(),
        "target", target,
        "method", request.getMethod().name(),
        "outcome", outcome
    ).increment();
  }

  private String source() {
    String applicationName = environment.getProperty("spring.application.name");
    return applicationName == null || applicationName.isBlank() ? "unknown" : applicationName;
  }

  private String target(URI uri) {
    String host = uri.getHost();
    if (host == null || host.isBlank()) {
      return "unknown";
    }
    if (host.contains("bot-io")) {
      return "bot-io";
    }
    if (host.contains("bot-engine")) {
      return "bot-engine";
    }
    if (host.contains("bot-web")) {
      return "bot-web";
    }
    return host;
  }
}
