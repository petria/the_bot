package org.freakz.hermesmanager.config;

import org.freakz.common.spring.security.ServiceTokenAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AnonymousAuthenticationFilter;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;

@Configuration
public class SecurityConfig {

  @Bean
  @Order(1)
  SecurityFilterChain apiSecurity(HttpSecurity http, HermesManagerProperties properties) throws Exception {
    return http
        .securityMatcher(PathPatternRequestMatcher.pathPattern("/api/**"))
        .csrf(AbstractHttpConfigurer::disable)
        .requestCache(AbstractHttpConfigurer::disable)
        .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .exceptionHandling(errors -> errors.authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
        .authorizeHttpRequests(auth -> auth.anyRequest().hasAuthority(ServiceTokenAuthenticationFilter.AUTHORITY))
        .addFilterBefore(new ServiceTokenAuthenticationFilter(
            HttpHeaders.AUTHORIZATION, "Bearer ", properties.token(), "bot-engine"),
            AnonymousAuthenticationFilter.class)
        .build();
  }

  @Bean
  SecurityFilterChain healthSecurity(HttpSecurity http) throws Exception {
    return http.authorizeHttpRequests(auth -> auth
        .requestMatchers("/actuator/health").permitAll()
        .anyRequest().denyAll())
        .build();
  }
}
