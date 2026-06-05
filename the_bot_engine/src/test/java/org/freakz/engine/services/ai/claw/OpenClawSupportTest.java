package org.freakz.engine.services.ai.claw;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Base64;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OpenClawSupportTest {

  @Test
  void convertsOpenClawBootstrapPropertyKeys() {
    assertThat(OpenClawConfigSupport.toBootstrapPropertyKey("openclawGatewayWsUrl"))
        .isEqualTo("openclaw.gateway-ws-url");
    assertThat(OpenClawConfigSupport.toBootstrapPropertyKey("gatewayToken"))
        .isEqualTo("openclaw.gateway-token");
    assertThat(OpenClawConfigSupport.toBootstrapPropertyKey("openclaw"))
        .isEqualTo("openclaw");
  }

  @Test
  void buildsDeviceSignaturePayloadWithEmptyNullValues() {
    assertThat(OpenClawDeviceCrypto.buildDeviceSignaturePayload(
        "device",
        null,
        "backend",
        "operator",
        List.of("operator.read", "operator.write"),
        123L,
        null,
        "nonce"))
        .isEqualTo("v2|device||backend|operator|operator.read,operator.write|123||nonce");
  }

  @Test
  void extractsLastThirtyTwoBytesFromPublicKeyPem() throws Exception {
    byte[] encoded = new byte[44];
    for (int i = 0; i < encoded.length; i++) {
      encoded[i] = (byte) i;
    }
    byte[] raw = new byte[32];
    System.arraycopy(encoded, encoded.length - 32, raw, 0, raw.length);
    String pem = "-----BEGIN PUBLIC KEY-----\n"
        + Base64.getEncoder().encodeToString(encoded)
        + "\n-----END PUBLIC KEY-----";

    assertThat(OpenClawDeviceCrypto.extractRawPublicKey(pem))
        .isEqualTo(Base64.getUrlEncoder().withoutPadding().encodeToString(raw));
  }

  @Test
  void rejectsTooShortPublicKeyPem() {
    String pem = "-----BEGIN PUBLIC KEY-----\n"
        + Base64.getEncoder().encodeToString(new byte[31])
        + "\n-----END PUBLIC KEY-----";

    assertThatThrownBy(() -> OpenClawDeviceCrypto.extractRawPublicKey(pem))
        .isInstanceOf(IOException.class)
        .hasMessageContaining("failed to parse device public key");
  }
}
