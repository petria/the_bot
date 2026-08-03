package org.freakz.common.spring.security;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.web.filter.OncePerRequestFilter;

import org.freakz.common.spring.rest.InternalApiTokenInterceptor;

/** Protects private service endpoints from callers outside the trusted service clients. */
public final class InternalApiTokenFilter extends OncePerRequestFilter {

  private final String expectedToken;
  private final List<String> protectedPrefixes;
  private final List<String> excludedPrefixes;

  public InternalApiTokenFilter(
      String expectedToken,
      List<String> protectedPrefixes,
      List<String> excludedPrefixes) {
    this.expectedToken = expectedToken == null ? "" : expectedToken.trim();
    this.protectedPrefixes = List.copyOf(protectedPrefixes);
    this.excludedPrefixes = List.copyOf(excludedPrefixes);
  }

  @Override
  protected boolean shouldNotFilter(HttpServletRequest request) {
    String path = request.getRequestURI();
    return protectedPrefixes.stream().noneMatch(path::startsWith)
        || excludedPrefixes.stream().anyMatch(path::startsWith);
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request,
      HttpServletResponse response,
      FilterChain filterChain) throws ServletException, IOException {
    if (expectedToken.isBlank()) {
      response.sendError(HttpStatus.SERVICE_UNAVAILABLE.value(), "Internal API authentication is not configured");
      return;
    }

    String suppliedToken = request.getHeader(InternalApiTokenInterceptor.HEADER);
    if (!matches(expectedToken, suppliedToken)) {
      response.sendError(HttpStatus.UNAUTHORIZED.value(), "Invalid internal API token");
      return;
    }
    filterChain.doFilter(request, response);
  }

  private boolean matches(String expected, String supplied) {
    if (supplied == null) {
      return false;
    }
    return MessageDigest.isEqual(
        expected.getBytes(StandardCharsets.UTF_8),
        supplied.trim().getBytes(StandardCharsets.UTF_8));
  }
}
