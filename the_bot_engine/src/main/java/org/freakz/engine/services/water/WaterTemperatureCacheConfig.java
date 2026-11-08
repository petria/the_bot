package org.freakz.engine.services.water;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.Ticker;
import java.time.Duration;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableCaching
public class WaterTemperatureCacheConfig {
  @Bean
  public CaffeineCacheManager waterTemperatureCacheManager() {
    return create(Ticker.systemTicker());
  }

  static CaffeineCacheManager create(Ticker ticker) {
    var manager = new CaffeineCacheManager("waterTemperatures");
    manager.setAllowNullValues(false);
    manager.setCaffeine(Caffeine.newBuilder().maximumSize(1024)
        .expireAfterWrite(Duration.ofMinutes(60)).recordStats().ticker(ticker));
    return manager;
  }
}
