package org.freakz.engine.services.urls;

import org.junit.jupiter.api.Test;

import java.net.URI;

import static org.assertj.core.api.Assertions.assertThat;

class UrlSecurityValidatorTest {

  private final UrlSecurityValidator validator = new UrlSecurityValidator();

  @Test
  void rejectsLocalAndNonHttpUrls() {
    assertThat(validator.isAllowed(URI.create("http://127.0.0.1/test"))).isFalse();
    assertThat(validator.isAllowed(URI.create("http://localhost/test"))).isFalse();
    assertThat(validator.isAllowed(URI.create("http://100.64.0.1/test"))).isFalse();
    assertThat(validator.isAllowed(URI.create("http://192.0.2.1/test"))).isFalse();
    assertThat(validator.isAllowed(URI.create("http://[fd00::1]/test"))).isFalse();
    assertThat(validator.isAllowed(URI.create("file:///tmp/test"))).isFalse();
    assertThat(validator.isAllowed(URI.create("https://user:password@example.com/test"))).isFalse();
  }
}
