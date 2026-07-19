package org.freakz.web.mobile;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.freakz.web.security.BotUserPrincipal;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class MobileBearerAuthenticationFilter extends OncePerRequestFilter {
  private final MobileAuthService authService;

  public MobileBearerAuthenticationFilter(MobileAuthService authService) {
    this.authService = authService;
  }

  @Override
  protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
      throws ServletException, IOException {
    if (request.getRequestURI().startsWith("/api/mobile/")
        && !request.getRequestURI().startsWith("/api/mobile/auth/")) {
      SecurityContextHolder.clearContext();
    }
    String header = request.getHeader(HttpHeaders.AUTHORIZATION);
    if (header != null && header.startsWith("Bearer ")) {
      BotUserPrincipal principal = authService.authenticateAccessToken(header.substring(7).trim());
      if (principal != null) {
        var authentication = new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(authentication);
      }
    }
    chain.doFilter(request, response);
  }
}
