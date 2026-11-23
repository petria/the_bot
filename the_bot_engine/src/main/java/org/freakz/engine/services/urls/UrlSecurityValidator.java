package org.freakz.engine.services.urls;

import java.net.URI;

import org.freakz.common.net.PublicUrlPolicy;
import org.springframework.stereotype.Component;

@Component
public class UrlSecurityValidator {

  public boolean isAllowed(URI uri) {
    return PublicUrlPolicy.isAllowed(uri);
  }
}
