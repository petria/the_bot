package org.freakz.web.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.http.HttpStatus;
import tools.jackson.databind.json.JsonMapper;

import jakarta.servlet.http.HttpServletResponse;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

  @Bean
  public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    http
        .csrf(csrf -> csrf
            .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
            .ignoringRequestMatchers(PathPatternRequestMatcher.pathPattern("/logout"))
        )
        .authorizeHttpRequests(authorize -> authorize
            .requestMatchers("/assets/**", "/favicon.ico", "/default-ui.css", "/error").permitAll()
            .requestMatchers("/api/web/admin/**").hasRole("ADMIN")
            .requestMatchers("/api/web/**").authenticated()
            .anyRequest().authenticated()
        )
        .exceptionHandling(exceptionHandling -> exceptionHandling
            .defaultAuthenticationEntryPointFor(
                apiAuthenticationEntryPoint(),
                PathPatternRequestMatcher.pathPattern("/api/web/**"))
        )
        .formLogin(formLogin -> formLogin
            .successHandler((request, response, authentication) -> relativeRedirect(response, "/"))
            .permitAll()
        )
        .logout(logout -> logout
            .logoutUrl("/logout")
            .logoutSuccessHandler((request, response, authentication) -> relativeRedirect(response, "/login?logout"))
            .permitAll()
        );
    return http.build();
  }

  private void relativeRedirect(HttpServletResponse response, String location) {
    response.setStatus(HttpServletResponse.SC_FOUND);
    response.setHeader("Location", location);
  }

  @Bean
  public AuthenticationEntryPoint apiAuthenticationEntryPoint() {
    return new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED);
  }

  @Bean
  public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }

  @Bean
  public JsonMapper jsonMapper() {
    return JsonMapper.builder().build();
  }
}
