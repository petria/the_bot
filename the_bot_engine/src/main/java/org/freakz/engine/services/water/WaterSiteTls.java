package org.freakz.engine.services.water;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.security.GeneralSecurityException;
import java.security.cert.X509Certificate;

/** Compatibility TLS context for the fixed legacy SYKE water-data host. */
public final class WaterSiteTls {

  private WaterSiteTls() {
  }

  public static SSLContext context() {
    try {
      TrustManager[] trustManagers = {new X509TrustManager() {
        public X509Certificate[] getAcceptedIssuers() {
          return new X509Certificate[0];
        }

        public void checkClientTrusted(X509Certificate[] chain, String authType) {
        }

        public void checkServerTrusted(X509Certificate[] chain, String authType) {
        }
      }};
      SSLContext context = SSLContext.getInstance("TLS");
      context.init(null, trustManagers, new java.security.SecureRandom());
      return context;
    } catch (GeneralSecurityException error) {
      throw new IllegalStateException("Could not initialize SYKE TLS compatibility", error);
    }
  }
}
