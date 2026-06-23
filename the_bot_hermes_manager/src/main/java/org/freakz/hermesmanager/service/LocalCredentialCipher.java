package org.freakz.hermesmanager.service;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.freakz.hermesmanager.config.HermesManagerProperties;
import org.springframework.stereotype.Component;

@Component
public class LocalCredentialCipher {

  private static final String PREFIX = "aesgcm:";
  private static final int IV_LENGTH = 12;
  private static final int TAG_BITS = 128;

  private final byte[] key;
  private final SecureRandom secureRandom = new SecureRandom();

  public LocalCredentialCipher(HermesManagerProperties properties) {
    String encodedKey = properties.localCredentialKey();
    if (encodedKey == null || encodedKey.isBlank()) {
      this.key = null;
      return;
    }
    byte[] decoded = Base64.getDecoder().decode(encodedKey.trim());
    if (decoded.length != 32) {
      throw new IllegalArgumentException(
          "HERMES_MANAGER_LOCAL_CREDENTIAL_KEY must be a Base64-encoded 256-bit key");
    }
    this.key = decoded;
  }

  public String encrypt(String plaintext) {
    if (plaintext == null || plaintext.isBlank()) {
      return null;
    }
    requireKey();
    try {
      byte[] iv = new byte[IV_LENGTH];
      secureRandom.nextBytes(iv);
      Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
      cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(key, "AES"), new GCMParameterSpec(TAG_BITS, iv));
      byte[] encrypted = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
      byte[] payload = new byte[iv.length + encrypted.length];
      System.arraycopy(iv, 0, payload, 0, iv.length);
      System.arraycopy(encrypted, 0, payload, iv.length, encrypted.length);
      return PREFIX + Base64.getEncoder().encodeToString(payload);
    } catch (Exception e) {
      throw new IllegalStateException("Could not encrypt local LLM credential", e);
    }
  }

  public String decrypt(String encrypted) {
    if (encrypted == null || encrypted.isBlank()) {
      return null;
    }
    if (!encrypted.startsWith(PREFIX)) {
      throw new IllegalArgumentException("Unsupported local LLM credential format");
    }
    requireKey();
    try {
      byte[] payload = Base64.getDecoder().decode(encrypted.substring(PREFIX.length()));
      byte[] iv = java.util.Arrays.copyOfRange(payload, 0, IV_LENGTH);
      byte[] ciphertext = java.util.Arrays.copyOfRange(payload, IV_LENGTH, payload.length);
      Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
      cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(key, "AES"), new GCMParameterSpec(TAG_BITS, iv));
      return new String(cipher.doFinal(ciphertext), StandardCharsets.UTF_8);
    } catch (Exception e) {
      throw new IllegalStateException("Could not decrypt local LLM credential", e);
    }
  }

  private void requireKey() {
    if (key == null) {
      throw new IllegalStateException(
          "HERMES_MANAGER_LOCAL_CREDENTIAL_KEY is required to store local LLM API keys");
    }
  }
}
