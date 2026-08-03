package org.freakz.io;

import org.freakz.common.config.TheBotProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.core.Ordered;
import org.freakz.common.spring.security.InternalApiTokenFilter;

import java.util.List;
import java.util.TimeZone;

@SpringBootApplication
@EnableConfigurationProperties(TheBotProperties.class)
@EnableAsync
@EnableScheduling
@ComponentScan(basePackages = {"org.freakz.io", "org.freakz.common.spring"})
public class SpringApplicationBotIo {

  static void main(String[] args) {

    String timezone = System.getProperty("TZ", "Europe/Helsinki");
    System.out.printf("Setting default timezone: %s", timezone);
    TimeZone.setDefault(TimeZone.getTimeZone(timezone));

    SpringApplication.run(SpringApplicationBotIo.class, args);
  }

  @org.springframework.context.annotation.Bean
  FilterRegistrationBean<InternalApiTokenFilter> internalApiTokenFilter(
      @Value("${the.bot.internal-api-token:}") String token) {
    FilterRegistrationBean<InternalApiTokenFilter> registration =
        new FilterRegistrationBean<>(new InternalApiTokenFilter(
            token,
            List.of("/api/hokan/io/"),
            List.of()));
    registration.addUrlPatterns("/api/hokan/io/*");
    registration.setOrder(Ordered.HIGHEST_PRECEDENCE);
    return registration;
  }
}
