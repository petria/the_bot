package org.freakz.engine;

import org.freakz.common.config.TheBotProperties;
import org.freakz.engine.services.weather.weatherapi.WeatherConfigProperties;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.TimeZone;
import java.util.concurrent.Executor;

@EnableConfigurationProperties({WeatherConfigProperties.class, TheBotProperties.class})
@SpringBootApplication
@EnableAsync
@EnableScheduling
@ComponentScan(basePackages = {"org.freakz.engine", "org.freakz.common.spring"})
public class SpringApplicationBotEngine {


  public static void main(String[] args) {
    String timezone = System.getProperty("TZ", "Europe/Helsinki");
    System.out.printf("Setting default timezone: %s", timezone);
    TimeZone.setDefault(TimeZone.getTimeZone(timezone));

    SpringApplication.run(SpringApplicationBotEngine.class, args);
  }

  @Bean
  public Executor taskExecutor() {
    ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
    executor.setCorePoolSize(10);
    executor.setMaxPoolSize(10);
    executor.setQueueCapacity(500);
    executor.setThreadNamePrefix("BotEngine-");
    executor.initialize();
    return executor;
  }

  @Bean
  @Qualifier("Services")
  public Executor taskServiceExecutor() {
    ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
    executor.setCorePoolSize(10);
    executor.setMaxPoolSize(10);
    executor.setQueueCapacity(500);
    executor.setThreadNamePrefix("BotServices-");
    executor.initialize();
    return executor;
  }
}
