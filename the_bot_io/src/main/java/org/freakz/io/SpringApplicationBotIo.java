package org.freakz.io;

import org.freakz.common.config.TheBotProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

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
}
