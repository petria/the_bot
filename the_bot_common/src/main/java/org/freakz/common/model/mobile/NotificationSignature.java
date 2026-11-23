package org.freakz.common.model.mobile;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.util.HexFormat;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

public final class NotificationSignature {

  public static final String HEADER = "X-TheBot-Notification-Signature";

  private NotificationSignature() {}

  public static String sign(String key, byte[] body) {
    try {
      Mac mac = Mac.getInstance("HmacSHA256");
      mac.init(new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
      return HexFormat.of().formatHex(mac.doFinal(body));
    } catch (GeneralSecurityException e) {
      throw new IllegalStateException("Could not sign notification", e);
    }
  }

  public static boolean matches(String key, byte[] body, String signature) {
    return signature != null && signature.length() == 64 && MessageDigest.isEqual(
        sign(key, body).getBytes(StandardCharsets.UTF_8), signature.getBytes(StandardCharsets.UTF_8));
  }
}
