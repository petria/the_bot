package org.freakz.common.spring.rest;

import java.util.ArrayList;

import org.springframework.web.client.RestTemplate;

/** Creates a RestTemplate carrying the internal service authentication header. */
public final class InternalRestTemplate {

  private InternalRestTemplate() {
  }

  public static RestTemplate withToken(RestTemplate base, String token) {
    RestTemplate internal = new RestTemplate(base.getRequestFactory());
    var interceptors = new ArrayList<>(base.getInterceptors());
    interceptors.add(new InternalApiTokenInterceptor(token));
    internal.setInterceptors(interceptors);
    return internal;
  }
}
