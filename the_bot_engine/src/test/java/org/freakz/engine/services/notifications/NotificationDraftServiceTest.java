package org.freakz.engine.services.notifications;

import org.freakz.common.model.engine.EngineRequest;
import org.freakz.common.model.users.User;
import org.freakz.engine.services.ai.hermes.HermesAiService;
import org.freakz.engine.services.livechannel.LiveChannelEventService;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.util.concurrent.TimeUnit;

class NotificationDraftServiceTest {
  private EngineRequest message(String alias, String text, long id) {
    var r = EngineRequest.builder().echoToAlias(alias).command(text).network("IRCNet").fromSender("friend").timestamp(id).build();
    return r;
  }
  @Test void snapshotsOnlyEarlierMessagesInSameChannelAndBoundsContext() {
    var events = new LiveChannelEventService();
    for (int i=0; i<50; i++) events.recordInbound(message("IRC", "line-" + i, i));
    events.recordInbound(message("OTHER", "secret-other-channel", 51));
    var trigger = message("IRC", "  Pete: question  ", 52);
    events.recordInbound(trigger);
    events.recordInbound(message("IRC", "future-message", 53));
    try (var service = new DraftCloseable(mock(HermesAiService.class), events)) {
      String input=service.value.capture(User.builder().username("pete").build(), trigger, trigger.getMessage().trim());
      assertThat(input).contains("line-49", "line-10", "Pete: question").doesNotContain("line-9\n", "secret-other-channel", "future-message");
      assertThat(input.indexOf("Pete: question")).isEqualTo(input.lastIndexOf("Pete: question"));
    }
  }
  @Test void capsLargeContextAndExcludesOldMessages() {
    var events=mock(LiveChannelEventService.class);
    long now=System.currentTimeMillis();
    var old=new org.freakz.common.model.engine.livechannel.LiveChannelEvent(1,1,now-TimeUnit.MINUTES.toMillis(31),"IRC","friend","id","expired-text","irc","IRCNet","channel","chat",org.freakz.common.model.engine.livechannel.LiveChannelDirection.INBOUND);
    var large=new org.freakz.common.model.engine.livechannel.LiveChannelEvent(2,2,now,"IRC","friend","id","x".repeat(20000),"irc","IRCNet","channel","chat",org.freakz.common.model.engine.livechannel.LiveChannelDirection.INBOUND);
    when(events.eventsAfter("IRC",0)).thenReturn(new org.freakz.common.model.engine.livechannel.LiveChannelEventsResponse(java.util.List.of(old,large)));
    try(var service=new DraftCloseable(mock(HermesAiService.class),events)) {
      var input=service.value.capture(User.builder().username("pete").build(),message("IRC","Pete: question",3),"Pete: question");
      assertThat(input).doesNotContain("expired-text").contains("Pete: question").hasSizeLessThan(12500);
    }
  }
  @Test void queueCapacityFallsBackInsteadOfBlockingIncomingMessages() throws Exception {
    var ai=mock(HermesAiService.class);
    var latch=new java.util.concurrent.CountDownLatch(1);
    when(ai.draftNotificationReply(anyString())).thenAnswer(inv -> { latch.await(); return "draft"; });
    var service=new NotificationDraftService(ai,new LiveChannelEventService());
    try {
      for(int i=0;i<102;i++) service.generate("context");
      assertThat(service.generate("overflow").get(1,TimeUnit.SECONDS)).isNull();
    } finally { latch.countDown(); service.close(); }
  }
  @Test void deadlineAndAiFailureReturnFallbackWithoutWaitingForBackend() throws Exception {
    var ai=mock(HermesAiService.class);
    when(ai.draftNotificationReply(anyString())).thenAnswer(inv -> { Thread.sleep(10000); return "late"; });
    var service=new NotificationDraftService(ai,new LiveChannelEventService(),100);
    try { assertThat(service.generate("context").get(2,TimeUnit.SECONDS)).isNull(); }
    finally { service.close(); }
    when(ai.draftNotificationReply(anyString())).thenThrow(new IllegalStateException("offline"));
    service=new NotificationDraftService(ai,new LiveChannelEventService());
    try { assertThat(service.generate("context").get(2,TimeUnit.SECONDS)).isNull(); }
    finally { service.close(); }
  }
  private static class DraftCloseable implements AutoCloseable {
    final NotificationDraftService value;
    DraftCloseable(HermesAiService ai, LiveChannelEventService events) { value=new NotificationDraftService(ai,events); }
    public void close() { value.close(); }
  }
}
