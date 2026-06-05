package org.freakz.engine.services.ai.claw;

import org.freakz.common.util.TextUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;
import java.util.List;

final class OpenClawDeviceCrypto {

  private OpenClawDeviceCrypto() {
  }

  static PrivateKey parsePrivateKey(String privateKeyPem) throws IOException {
    if (privateKeyPem == null || privateKeyPem.isBlank()) {
      throw new IOException("missing private key PEM");
    }

    try {
      String sanitized = privateKeyPem
          .replace("-----BEGIN PRIVATE KEY-----", "")
          .replace("-----END PRIVATE KEY-----", "")
          .replaceAll("\\s+", "");
      byte[] keyBytes = Base64.getDecoder().decode(sanitized);
      PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(keyBytes);
      return KeyFactory.getInstance("Ed25519").generatePrivate(spec);
    } catch (Exception e) {
      throw new IOException("failed to parse Ed25519 private key", e);
    }
  }

  static String signDevicePayload(
      PrivateKey privateKey,
      String deviceId,
      String clientId,
      String clientMode,
      String role,
      List<String> scopes,
      long signedAtMs,
      String token,
      String nonce,
      String failureMessage
  ) {
    try {
      String payload = buildDeviceSignaturePayload(deviceId, clientId, clientMode, role, scopes, signedAtMs, token, nonce);
      Signature signature = Signature.getInstance("Ed25519");
      signature.initSign(privateKey);
      signature.update(payload.getBytes(StandardCharsets.UTF_8));
      return Base64.getUrlEncoder().withoutPadding().encodeToString(signature.sign());
    } catch (Exception e) {
      throw new IllegalStateException(failureMessage, e);
    }
  }

  static String buildDeviceSignaturePayload(
      String deviceId,
      String clientId,
      String clientMode,
      String role,
      List<String> scopes,
      long signedAtMs,
      String token,
      String nonce
  ) {
    String joinedScopes = scopes == null || scopes.isEmpty() ? "" : String.join(",", scopes);
    return String.join("|",
        "v2",
        TextUtils.nullToEmpty(deviceId),
        TextUtils.nullToEmpty(clientId),
        TextUtils.nullToEmpty(clientMode),
        TextUtils.nullToEmpty(role),
        joinedScopes,
        Long.toString(signedAtMs),
        TextUtils.nullToEmpty(token),
        TextUtils.nullToEmpty(nonce)
    );
  }

  static String extractRawPublicKey(String publicKeyPem) throws IOException {
    if (publicKeyPem == null || publicKeyPem.isBlank()) {
      return "";
    }

    try {
      String sanitized = publicKeyPem
          .replace("-----BEGIN PUBLIC KEY-----", "")
          .replace("-----END PUBLIC KEY-----", "")
          .replaceAll("\\s+", "");
      byte[] encoded = Base64.getDecoder().decode(sanitized);
      if (encoded.length < 32) {
        throw new IOException("unexpected public key length");
      }
      byte[] raw = new byte[32];
      System.arraycopy(encoded, encoded.length - 32, raw, 0, 32);
      return Base64.getUrlEncoder().withoutPadding().encodeToString(raw);
    } catch (Exception e) {
      throw new IOException("failed to parse device public key", e);
    }
  }
}
