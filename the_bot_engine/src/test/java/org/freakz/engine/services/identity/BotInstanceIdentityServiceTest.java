package org.freakz.engine.services.identity;

import org.freakz.engine.config.ConfigService;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BotInstanceIdentityServiceTest {

  @Test
  void usesConfiguredInstanceId() {
    BotInstanceIdentityService service = new BotInstanceIdentityService(
        new TestConfigService("hokan-develop", "DEV"));

    assertThat(service.getInstanceId()).isEqualTo("hokan-develop");
  }

  @Test
  void fallsBackToActiveProfile() {
    BotInstanceIdentityService service = new BotInstanceIdentityService(
        new TestConfigService(null, "PROD"));

    assertThat(service.getInstanceId()).isEqualTo("prod");
  }

  @Test
  void rejectsUnsafeInstanceId() {
    BotInstanceIdentityService service = new BotInstanceIdentityService(
        new TestConfigService("../prod", "PROD"));

    assertThatThrownBy(service::getInstanceId)
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("Invalid bot instance identity");
  }

  private static class TestConfigService extends ConfigService {
    private final String instanceId;
    private final String activeProfile;
    TestConfigService(String instanceId, String activeProfile) {
      this.instanceId = instanceId;
      this.activeProfile = activeProfile;
    }

    @Override
    public String getActiveProfile() {
      return activeProfile;
    }

    @Override
    public String getConfigValue(String propertyKey, String envKey, String defaultValue) {
      if ("hokan.bot.instance-id".equals(propertyKey)) {
        return instanceId;
      }
      return defaultValue;
    }
  }
}
