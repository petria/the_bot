package org.freakz.web;

import org.freakz.common.config.TheBotProperties;
import org.freakz.web.config.TheBotWebProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.ComponentScan;

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
}
