package org.freakz.common.spring.rest;

import java.io.IOException;

import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;

/** Adds the service-to-service token to requests made by internal clients only. */
public final class InternalApiTokenInterceptor implements ClientHttpRequestInterceptor {

  public static final String HEADER = "X-TheBot-Internal-Token";

  private final String token;

  public InternalApiTokenInterceptor(String token) {
    this.token = token == null ? "" : token.trim();
  }

  @Override
  public ClientHttpResponse intercept(
      HttpRequest request,
      byte[] body,
      ClientHttpRequestExecution execution) throws IOException {
    request.getHeaders().set(HEADER, token);
    return execution.execute(request, body);
  }
}
