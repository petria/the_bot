package org.freakz.io;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.ServletComponentScan;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.TimeZone;

@SpringBootApplication
@EnableAsync
@EnableFeignClients
@EnableScheduling
@ServletComponentScan
public class SpringApplicationBotIo {


    public static void main(String[] args) {

        String timezone = System.getProperty("TZ", "Europe/Helsinki");
        System.out.printf("Setting default timezone: %s", timezone);
        TimeZone.setDefault(TimeZone.getTimeZone(timezone));

        SpringApplication.run(SpringApplicationBotIo.class, args);
    }
}