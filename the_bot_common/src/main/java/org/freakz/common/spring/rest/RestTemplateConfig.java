package org.freakz.common.spring.rest;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

@Configuration
public class RestTemplateConfig {

  @Bean
  public RestTemplate restTemplate(ObjectProvider<MeterRegistry> meterRegistry, Environment environment) {
    SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
    requestFactory.setConnectTimeout(Duration.ofSeconds(2));
    requestFactory.setReadTimeout(Duration.ofSeconds(30));

    RestTemplate restTemplate = new RestTemplate(requestFactory);
    MeterRegistry registry = meterRegistry.getIfAvailable();
    if (registry != null) {
      List<ClientHttpRequestInterceptor> interceptors = new ArrayList<>(restTemplate.getInterceptors());
      interceptors.add(new RestClientMetricsInterceptor(registry, environment));
      restTemplate.setInterceptors(interceptors);
    }
    return restTemplate;
  }
}
