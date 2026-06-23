package org.freakz.hermesmanager.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.file.Path;
import java.util.Base64;
import java.util.List;

import org.freakz.hermesmanager.config.HermesManagerProperties;
import org.junit.jupiter.api.Test;

class LocalCredentialCipherTest {

  @Test
  void encryptsAndDecryptsCredentialWithoutStoringPlaintext() {
    LocalCredentialCipher cipher = new LocalCredentialCipher(properties(key()));

    String encrypted = cipher.encrypt("secret-token");

    assertThat(encrypted).startsWith("aesgcm:").doesNotContain("secret-token");
    assertThat(cipher.decrypt(encrypted)).isEqualTo("secret-token");
  }

  @Test
  void rejectsCredentialWritesWhenEncryptionKeyIsMissing() {
    LocalCredentialCipher cipher = new LocalCredentialCipher(properties(""));

    assertThatThrownBy(() -> cipher.encrypt("secret-token"))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("HERMES_MANAGER_LOCAL_CREDENTIAL_KEY");
  }

  private HermesManagerProperties properties(String encryptionKey) {
    return new HermesManagerProperties(
        Path.of("."),
        "test",
        List.of("chat"),
        "chat:8643",
        "http://localhost:11434/v1",
        "model",
        "token",
        encryptionKey);
  }

  private String key() {
    return Base64.getEncoder().encodeToString(new byte[32]);
  }
}
