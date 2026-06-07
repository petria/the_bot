package org.freakz.engine.services.urls.client;

import org.freakz.engine.services.urls.UrlResolverProperties;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

class UrlProviderClientConfigurationTest {

  private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
      .withUserConfiguration(UrlProviderClientConfiguration.class, UrlResolverProperties.class);

  @Test
  void createsProviderClientsWithoutAutoConfiguredRestClientBuilder() {
    contextRunner.run(context -> {
      assertThat(context).hasSingleBean(YouTubeApiClient.class);
      assertThat(context).hasSingleBean(WikipediaApiClient.class);
    });
  }
}
