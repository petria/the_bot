package org.freakz.engine.services.ai.claw;

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

  @Test
  void buildsInstanceMount() {
    BotInstanceIdentityService service = new BotInstanceIdentityService(
        new TestConfigService("hokan-main", "PROD", "/mnt/hokan/"));

    assertThat(service.getInstanceMount()).isEqualTo("/mnt/hokan/hokan-main");
  }

  private static class TestConfigService extends ConfigService {
    private final String instanceId;
    private final String activeProfile;
    private final String baseMount;

    TestConfigService(String instanceId, String activeProfile) {
      this(instanceId, activeProfile, "/mnt/hokan");
    }

    TestConfigService(String instanceId, String activeProfile, String baseMount) {
      this.instanceId = instanceId;
      this.activeProfile = activeProfile;
      this.baseMount = baseMount;
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
      if ("openclaw.external-base-mount".equals(propertyKey)) {
        return baseMount;
      }
      return defaultValue;
    }
  }
}
