package org.freakz.cli.config;


import org.freakz.cli.service.CallCountInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

@Configuration
@EnableAspectJAutoProxy
public class AppConfiguration {

    @Bean
    public CallCountInterceptor callCountInterceptor() {
        return new CallCountInterceptor();
    }

}
