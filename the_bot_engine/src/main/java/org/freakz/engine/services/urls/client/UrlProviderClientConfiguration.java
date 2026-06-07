package org.freakz.engine.services.urls.client;

import org.freakz.engine.services.urls.UrlResolverProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

import java.net.http.HttpClient;
import java.time.Duration;

@Configuration
public class UrlProviderClientConfiguration {

  @Bean
  YouTubeApiClient youTubeApiClient(UrlResolverProperties properties) {
    RestClient restClient = providerRestClient(properties)
        .baseUrl("https://www.googleapis.com")
        .build();
    HttpServiceProxyFactory factory = HttpServiceProxyFactory
        .builderFor(RestClientAdapter.create(restClient))
        .build();
    return factory.createClient(YouTubeApiClient.class);
  }

  @Bean
  WikipediaApiClient wikipediaApiClient(UrlResolverProperties properties) {
    HttpServiceProxyFactory factory = HttpServiceProxyFactory
        .builderFor(RestClientAdapter.create(providerRestClient(properties).build()))
        .build();
    return factory.createClient(WikipediaApiClient.class);
  }

  private RestClient.Builder providerRestClient(UrlResolverProperties properties) {
    HttpClient httpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofMillis(properties.getConnectTimeoutMillis()))
        .followRedirects(HttpClient.Redirect.NEVER)
        .build();
    JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
    requestFactory.setReadTimeout(Duration.ofMillis(properties.getReadTimeoutMillis()));
    return RestClient.builder().requestFactory(requestFactory);
  }
}
