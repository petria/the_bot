package org.freakz.common.irc;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class IrcChannelModeSpecTest {

  @Test
  void normalizesEnabledParameterlessModes() {
    assertThat(IrcChannelModeSpec.parse("+tsst").value()).isEqualTo("+st");
  }

  @Test
  void blankValueRepresentsNoGuardedModes() {
    assertThat(IrcChannelModeSpec.parse(" ").value()).isEmpty();
  }

  @Test
  void rejectsModeParametersAndRemovalSyntax() {
    assertThatThrownBy(() -> IrcChannelModeSpec.parse("+k secret"))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> IrcChannelModeSpec.parse("-st"))
        .isInstanceOf(IllegalArgumentException.class);
  }
}
