package org.freakz.engine.services.ai;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AiReplyGuardTest {

  @Test
  void blocksRawJsonFinalAnswers() {
    assertThat(AiReplyGuard.safeFinalAnswer("{\"type\":\"tool\"}", "blocked")).isEqualTo("blocked");
    assertThat(AiReplyGuard.safeFinalAnswer("[{\"type\":\"tool\"}]", "blocked")).isEqualTo("blocked");
    assertThat(AiReplyGuard.safeFinalAnswer("""
        ```json
        {"type":"final","answer":"pong"}
        ```
        """, "blocked")).isEqualTo("blocked");
  }

  @Test
  void allowsPlainFinalAnswers() {
    assertThat(AiReplyGuard.safeFinalAnswer("pong", "blocked")).isEqualTo("pong");
  }

  @Test
  void blocksProtocolEnvelopeAppendedToPlainText() {
    assertThat(AiReplyGuard.safeFinalAnswer(
        "Checking logs. {\"type\":\"tool\",\"tool\":\"logs.search\",\"arguments\":{\"query\":\"test\"}}",
        "blocked"))
        .isEqualTo("blocked");
  }

  @Test
  void hidesStructuredErrorBodies() {
    assertThat(AiReplyGuard.safeFailure("AI command failed:", "500 body={\"error\":\"quota\"}"))
        .isEqualTo("AI command failed: upstream returned a structured error.");
  }
}
