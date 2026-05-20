package org.freakz.common.users;

import org.freakz.common.model.users.IrcClaimToken;
import org.freakz.common.model.users.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import tools.jackson.databind.json.JsonMapper;

import java.nio.file.Path;
import java.time.Duration;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class IrcClaimTokenStoreTest {

  @TempDir
  private Path tempDir;

  @Test
  void consumesValidTokenOnce() {
    IrcClaimTokenStore store = new IrcClaimTokenStore(tempDir.resolve("irc-claim-tokens.json"), JsonMapper.builder().build());
    IrcClaimTokenStore.CreatedToken created = store.createToken(user(), Duration.ofMinutes(15));

    Optional<IrcClaimToken> consumed = store.consume(created.token());

    assertThat(consumed).isPresent();
    assertThat(consumed.get().getUsername()).isEqualTo("petria");
    assertThat(store.consume(created.token())).isEmpty();
  }

  @Test
  void rejectsExpiredToken() {
    IrcClaimTokenStore store = new IrcClaimTokenStore(tempDir.resolve("irc-claim-tokens.json"), JsonMapper.builder().build());
    IrcClaimTokenStore.CreatedToken created = store.createToken(user(), Duration.ofMillis(-1));

    assertThat(store.consume(created.token())).isEmpty();
  }

  private User user() {
    User user = User.builder()
        .username("petria")
        .build();
    user.setId(1L);
    return user;
  }
}
