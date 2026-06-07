package org.freakz.engine.services.urls.resolver;

import org.freakz.engine.services.urls.UrlResolution;

import java.net.URI;
import java.util.Optional;

public interface UrlResolver {

  boolean supports(URI uri);

  Optional<UrlResolution> resolve(URI uri);
}
