package org.freakz.common.spring.security;

import java.io.IOException;
import java.util.List;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

/** Establishes an identity for a service token accepted by a stateless security chain. */
public final class ServiceTokenAuthenticationFilter extends InternalApiTokenFilter {

  public static final String AUTHORITY = "INTERNAL_SERVICE";

  private final String principal;

  public ServiceTokenAuthenticationFilter(String header, String prefix, String token, String principal) {
    super(header, prefix, token);
    this.principal = principal;
  }

  @Override
  protected void authenticated(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
      throws IOException, ServletException {
    var context = SecurityContextHolder.createEmptyContext();
    context.setAuthentication(UsernamePasswordAuthenticationToken.authenticated(
        principal, null, List.of(new SimpleGrantedAuthority(AUTHORITY))));
    SecurityContextHolder.setContext(context);
    try {
      chain.doFilter(request, response);
    } finally {
      SecurityContextHolder.clearContext();
    }
  }
}
