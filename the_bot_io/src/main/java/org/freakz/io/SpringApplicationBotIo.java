package org.freakz.io;

import java.util.TimeZone;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.ServletComponentScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableAsync
@EnableScheduling
@ServletComponentScan
@ComponentScan(basePackages = {"org.freakz.io", "org.freakz.common.spring.rest"})
public class SpringApplicationBotIo {

  public static void main(String[] args) {

    String timezone = System.getProperty("TZ", "Europe/Helsinki");
    System.out.printf("Setting default timezone: %s", timezone);
    TimeZone.setDefault(TimeZone.getTimeZone(timezone));

    SpringApplication.run(SpringApplicationBotIo.class, args);
  }
}
