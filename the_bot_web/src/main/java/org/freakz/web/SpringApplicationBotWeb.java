package org.freakz.web;

import org.freakz.common.config.TheBotProperties;
import org.freakz.web.config.TheBotWebProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Bean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.core.Ordered;
import org.freakz.common.spring.security.InternalApiTokenFilter;

import java.util.List;

import java.util.TimeZone;

@SpringBootApplication
@EnableConfigurationProperties({TheBotWebProperties.class, TheBotProperties.class})
@ComponentScan(basePackages = {"org.freakz.web", "org.freakz.common.spring"})
public class SpringApplicationBotWeb {

  public static void main(String[] args) {
    String timezone = System.getProperty("TZ", "Europe/Helsinki");
    System.out.printf("Setting default timezone: %s", timezone);
    TimeZone.setDefault(TimeZone.getTimeZone(timezone));

    SpringApplication.run(SpringApplicationBotWeb.class, args);
  }

  @Bean
  public FilterRegistrationBean<InternalApiTokenFilter> internalApiTokenFilter(
      @Value("${the.bot.internal-api-token:}") String token) {
    FilterRegistrationBean<InternalApiTokenFilter> registration =
        new FilterRegistrationBean<>(new InternalApiTokenFilter(token, List.of("/internal/"), List.of()));
    registration.addUrlPatterns("/internal/*");
    registration.setOrder(Ordered.HIGHEST_PRECEDENCE);
    return registration;
  }
}
