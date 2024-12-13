package org.freakz.cli;

import ch.qos.logback.classic.Level;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@SpringBootApplication
@EnableAsync
@EnableFeignClients
@EnableScheduling
public class SpringApplicationBotCli {

  @Bean
  public Executor taskExecutor() {
    ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
    executor.setCorePoolSize(10);
    executor.setMaxPoolSize(10);
    executor.setQueueCapacity(500);
    executor.setThreadNamePrefix("BotCli-");
    executor.initialize();
    return executor;
  }

  public static void main(String[] args) {
    // System.out.println("Hello world!");
    try {
      ch.qos.logback.classic.Logger root =
          (ch.qos.logback.classic.Logger)
              org.slf4j.LoggerFactory.getLogger(ch.qos.logback.classic.Logger.ROOT_LOGGER_NAME);
      root.setLevel(Level.OFF);

      SpringApplication app = new SpringApplication(SpringApplicationBotCli.class);
      //            app.setBannerMode(Banner.Mode.OFF);
      app.run(args);

    } catch (Exception e) {
      // int foo = 0;
    }
  }
}
