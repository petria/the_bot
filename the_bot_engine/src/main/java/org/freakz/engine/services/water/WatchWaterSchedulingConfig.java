package org.freakz.engine.services.water;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

/** Keep the chart/vision request from blocking existing scheduled maintenance. */
@Configuration
public class WatchWaterSchedulingConfig {
  @Bean(name = "taskScheduler")
  public ThreadPoolTaskScheduler taskScheduler() {
    var scheduler = new ThreadPoolTaskScheduler();
    scheduler.setPoolSize(1);
    scheduler.setThreadNamePrefix("bot-scheduled-");
    return scheduler;
  }

  @Bean(name = "watchWaterScheduler")
  public ThreadPoolTaskScheduler watchWaterScheduler() {
    var scheduler = new ThreadPoolTaskScheduler();
    scheduler.setPoolSize(1);
    scheduler.setThreadNamePrefix("watch-water-");
    return scheduler;
  }
}
