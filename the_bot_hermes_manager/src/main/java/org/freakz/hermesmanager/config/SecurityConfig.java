package org.freakz.hermesmanager.config;

import java.io.IOException;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.preauth.AbstractPreAuthenticatedProcessingFilter;
import org.springframework.web.filter.OncePerRequestFilter;

@Configuration
public class SecurityConfig {

  @Bean
  SecurityFilterChain securityFilterChain(HttpSecurity http, HermesManagerProperties properties) throws Exception {
    http.csrf(csrf -> csrf.disable())
        .authorizeHttpRequests(auth -> auth
            .requestMatchers("/actuator/health").permitAll()
            .anyRequest().permitAll())
        .addFilterBefore(new BearerTokenFilter(properties.token()), AbstractPreAuthenticatedProcessingFilter.class);
    return http.build();
  }

  static class BearerTokenFilter extends OncePerRequestFilter {
    private final String token;

    BearerTokenFilter(String token) {
      this.token = token;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
        throws ServletException, IOException {
      if (!request.getRequestURI().startsWith("/api/")) {
        chain.doFilter(request, response);
        return;
      }
      if (token == null || token.isBlank()) {
        response.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE, "HERMES_MANAGER_TOKEN is not configured");
        return;
      }
      if (!("Bearer " + token).equals(request.getHeader("Authorization"))) {
        response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
        return;
      }
      chain.doFilter(request, response);
    }
  }
}
