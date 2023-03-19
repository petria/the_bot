package org.freakz;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
@EnableFeignClients
public class SpringApplicationBotIo {


    public static void main(String[] args) {
        //System.out.println("Hello world!");
        org.springframework.boot.SpringApplication.run(SpringApplicationBotIo.class, args);
    }
}