package org.freakz.common.spring.security;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.web.filter.OncePerRequestFilter;

import org.freakz.common.spring.rest.InternalApiTokenInterceptor;

/** Protects private service endpoints from callers outside the trusted service clients. */
public class InternalApiTokenFilter extends OncePerRequestFilter {

  private final String header;
  private final boolean configured;
  private final byte[] expectedToken;

  public InternalApiTokenFilter(String expectedToken) {
    this(InternalApiTokenInterceptor.HEADER, "", expectedToken == null ? "" : expectedToken.trim());
  }

  protected InternalApiTokenFilter(String header, String prefix, String token) {
    this.header = header;
    this.configured = token != null && !token.isBlank();
    this.expectedToken = configured ? (prefix + token).getBytes(StandardCharsets.UTF_8) : new byte[0];
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request,
      HttpServletResponse response,
      FilterChain filterChain) throws ServletException, IOException {
    if (!configured) {
      response.setStatus(HttpStatus.SERVICE_UNAVAILABLE.value());
      return;
    }

    String suppliedToken = request.getHeader(header);
    if (suppliedToken == null || !MessageDigest.isEqual(
        expectedToken, suppliedToken.trim().getBytes(StandardCharsets.UTF_8))) {
      response.setStatus(HttpStatus.UNAUTHORIZED.value());
      return;
    }
    authenticated(request, response, filterChain);
  }

  protected void authenticated(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
      throws IOException, ServletException {
    chain.doFilter(request, response);
  }
}
