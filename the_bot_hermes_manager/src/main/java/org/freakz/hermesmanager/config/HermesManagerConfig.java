package org.freakz.hermesmanager.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import java.time.Duration;

@Configuration
@EnableConfigurationProperties(HermesManagerProperties.class)
public class HermesManagerConfig {

  @Bean
  public RestTemplate restTemplate(HermesManagerProperties properties) {
    SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
    requestFactory.setConnectTimeout(Duration.ofSeconds(3));
    requestFactory.setReadTimeout(Duration.ofSeconds(properties.resolvedLocalValidationTimeoutSeconds()));
    return new RestTemplate(requestFactory);
  }

}
