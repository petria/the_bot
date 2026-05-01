package org.freakz.web;

import org.freakz.web.config.TheBotWebProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

import java.util.TimeZone;

@SpringBootApplication
@EnableConfigurationProperties(TheBotWebProperties.class)
public class SpringApplicationBotWeb {

  public static void main(String[] args) {
    String timezone = System.getProperty("TZ", "Europe/Helsinki");
    System.out.printf("Setting default timezone: %s", timezone);
    TimeZone.setDefault(TimeZone.getTimeZone(timezone));

    SpringApplication.run(SpringApplicationBotWeb.class, args);
  }
}
