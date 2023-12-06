package org.freakz;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableAsync;

import java.util.TimeZone;

@SpringBootApplication
@EnableAsync
@EnableFeignClients
public class SpringApplicationBotIo {


    public static void main(String[] args) {

        String timezone = System.getProperty("TZ", "Europe/Helsinki");
        System.out.printf("Setting default timezone: %s", timezone);
        TimeZone.setDefault(TimeZone.getTimeZone(timezone));

        SpringApplication.run(SpringApplicationBotIo.class, args);
    }
}