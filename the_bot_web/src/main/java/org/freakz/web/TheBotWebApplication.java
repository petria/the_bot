package org.freakz.web;

import org.freakz.web.config.TheBotWebProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(TheBotWebProperties.class)
public class TheBotWebApplication {

  public static void main(String[] args) {
    SpringApplication.run(TheBotWebApplication.class, args);
  }
}
