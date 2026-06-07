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
  YouTubeApiClient youTubeApiClient(RestClient.Builder builder, UrlResolverProperties properties) {
    RestClient restClient = providerRestClient(builder, properties)
        .baseUrl("https://www.googleapis.com")
        .build();
    HttpServiceProxyFactory factory = HttpServiceProxyFactory
        .builderFor(RestClientAdapter.create(restClient))
        .build();
    return factory.createClient(YouTubeApiClient.class);
  }

  @Bean
  WikipediaApiClient wikipediaApiClient(RestClient.Builder builder, UrlResolverProperties properties) {
    HttpServiceProxyFactory factory = HttpServiceProxyFactory
        .builderFor(RestClientAdapter.create(providerRestClient(builder, properties).build()))
        .build();
    return factory.createClient(WikipediaApiClient.class);
  }

  private RestClient.Builder providerRestClient(
      RestClient.Builder builder,
      UrlResolverProperties properties) {
    HttpClient httpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofMillis(properties.getConnectTimeoutMillis()))
        .followRedirects(HttpClient.Redirect.NEVER)
        .build();
    JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
    requestFactory.setReadTimeout(Duration.ofMillis(properties.getReadTimeoutMillis()));
    return builder.clone().requestFactory(requestFactory);
  }
}
