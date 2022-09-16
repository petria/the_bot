package org.freakz.springboot.ui.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients
public class SpringApplicationUiBackend {

    public static void main(String[] args) {
        SpringApplication.run(SpringApplicationUiBackend.class, args);
    }

}
