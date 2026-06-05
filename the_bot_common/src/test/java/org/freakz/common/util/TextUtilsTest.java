package org.freakz.common.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TextUtilsTest {

  @Test
  void trimsAndLowercasesWithExpectedNullOrEmptyFallbacks() {
    assertThat(TextUtils.nullToEmpty(null)).isEqualTo("");
    assertThat(TextUtils.trimToNull("  value  ")).isEqualTo("value");
    assertThat(TextUtils.trimToNull("  ")).isNull();
    assertThat(TextUtils.trimToEmpty(null)).isEqualTo("");
    assertThat(TextUtils.lowerTrimToNull("  VaLuE  ")).isEqualTo("value");
    assertThat(TextUtils.lowerTrimToEmpty(null)).isEqualTo("");
  }

  @Test
  void findsFirstNonBlankValue() {
    assertThat(TextUtils.firstNonBlank(null, " ", " value ", "other")).isEqualTo(" value ");
    assertThat(TextUtils.firstNonBlank(null, " ")).isNull();
  }

  @Test
  void collapsesWhitespaceBeforeAbbreviating() {
    assertThat(TextUtils.collapseWhitespace(" first\n\tsecond  third ")).isEqualTo("first second third");
    assertThat(TextUtils.abbreviate(" first\nsecond third ", 12)).isEqualTo("first sec...");
    assertThat(TextUtils.abbreviate("short", 12)).isEqualTo("short");
    assertThat(TextUtils.abbreviate(null, 12)).isEqualTo("");
  }
}
