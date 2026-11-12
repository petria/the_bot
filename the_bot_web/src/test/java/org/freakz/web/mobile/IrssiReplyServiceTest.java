package org.freakz.web.mobile;

import org.freakz.common.model.mobile.*;
import org.freakz.web.config.TheBotWebProperties;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import tools.jackson.databind.json.JsonMapper;
import java.nio.file.*;
import java.time.*;
import java.util.*;
import static org.assertj.core.api.Assertions.*;

class IrssiReplyServiceTest {
  @TempDir Path directory;
  MobileNotificationService notifications;
  IrssiReplyService service;
  JsonMapper mapper=JsonMapper.builder().findAndAddModules().build();
  TestClock clock=new TestClock();
  ReplyDraft draft=new ReplyDraft("draft","IRCNet","#channel","Alice","_Pete_: question","A suggested reply","_Pete_");
  @BeforeEach void setup(){
    var props=new TheBotWebProperties();props.setMobileNotificationsFile(directory.resolve("mobile.json").toString());
    notifications=new MobileNotificationService(mapper,props);
    notifications.accept(new MobileNotificationEvent("event","petria","RULE_MATCH","Notice","body","irc","alias",null,clock.instant(),draft));
    service=new IrssiReplyService(notifications,mapper,directory.resolve("agents.json"),clock);
  }
  IrssiReplyService.Credentials online(){
    var code=service.createPairing("petria","_Pete_");
    var auth=service.pair(code.code(),"test-host");
    service.poll(auth.token(),List.of(new IrssiReplyService.Connection("IRCnet","_Pete_",List.of("#channel"))));
    return auth;
  }
  @Test void pairingIsSingleUseExpiringAndCredentialIsScopedAndHashed() throws Exception {
    var code=service.createPairing("petria","_Pete_");String firstCode=code.code();var auth=service.pair(firstCode,"host");
    assertThatThrownBy(()->service.pair(firstCode,"host")).hasMessageContaining("invalid");
    assertThat(Files.readString(directory.resolve("agents.json"))).doesNotContain(auth.token());
    assertThatThrownBy(()->service.poll("not-a-token",List.of())).hasMessageContaining("Invalid agent");
    code=service.createPairing("petria","_Pete_");String expired=code.code();clock.advance(601);
    assertThatThrownBy(()->service.pair(expired,"host")).hasMessageContaining("expired");
  }
  @Test void fixedDestinationAndRepeatedTapsClaimOnlyOnce() {
    var auth=online();var job=service.submit("petria","draft","Thanks",false);
    assertThat(job.channel()).isEqualTo("#channel");assertThat(job.message()).isEqualTo("Alice: Thanks");
    assertThat(service.submit("petria","draft","a different edit",false).id()).isEqualTo(job.id());
    assertThat(service.poll(auth.token(),List.of()).job().id()).isEqualTo(job.id());
    assertThat(service.poll(auth.token(),List.of()).job()).isNull();
    service.acknowledge(auth.token(),job.id(),"SUBMITTED","Submitted through Irssi");
    assertThat(service.status("petria",job.id()).status()).isEqualTo("SUBMITTED");
    assertThat(service.submit("petria","draft","again",true).id()).isEqualTo(job.id());
  }
  @Test void ownerIsolationAndControlTextValidation() {
    online();
    assertThatThrownBy(()->service.submit("another-user","draft","reply",false)).hasMessageContaining("Draft not found");
    for(String text:List.of("hello\n/quit","hello\rPRIVMSG #other :evil","hello\u0001"," ","x".repeat(501)))
      assertThatThrownBy(()->service.submit("petria","draft",text,false)).hasMessageContaining("one line");
    var job=service.submit("petria","draft","/quit",false);
    assertThat(job.message()).isEqualTo("Alice: /quit"); // literal text, no command dispatcher
    assertThatThrownBy(()->service.status("another-user",job.id())).hasMessageContaining("not found");
    var other=service.pair(service.createPairing("another-user","OtherNick").code(),"other-host");
    assertThatThrownBy(()->service.acknowledge(other.token(),job.id(),"SUBMITTED",null)).hasMessageContaining("not found");
  }
  @Test void unavailableNickChannelAndHeartbeatDoNotQueue() {
    var auth=online();clock.advance(46);
    assertThatThrownBy(()->service.submit("petria","draft","reply",false)).hasMessageContaining("offline");
    service.poll(auth.token(),List.of(new IrssiReplyService.Connection("IRCnet","wrong",List.of("#channel"))));
    assertThat(service.availability("petria","draft").available()).isFalse();
    service.poll(auth.token(),List.of(new IrssiReplyService.Connection("other-network","_Pete_",List.of("#channel"))));
    assertThat(service.availability("petria","draft").available()).isFalse();
    service.poll(auth.token(),List.of(new IrssiReplyService.Connection("IRCnet","_Pete_",List.of("#other"))));
    assertThat(service.availability("petria","draft").available()).isFalse();
  }
  @Test void expiryUnknownDispatchAndExplicitRetry() {
    var auth=online();var first=service.submit("petria","draft","reply",false);clock.advance(61);
    assertThat(service.status("petria",first.id()).status()).isEqualTo("EXPIRED");
    auth=online();var second=service.submit("petria","draft","reply",true);
    assertThat(second.id()).isNotEqualTo(first.id());
    // The retry was assigned to the first online agent, as both advertise the same target.
    service.poll(auth.token(),List.of());clock.advance(61);
    assertThat(service.status("petria",second.id()).status()).isEqualTo("UNCERTAIN");
  }
  @Test void restartNeverRedeliversClaimedSendAndRevocationBlocksAgent() {
    var auth=online();var job=service.submit("petria","draft","reply",false);service.poll(auth.token(),List.of());
    var restart=new IrssiReplyService(notifications,mapper,directory.resolve("agents.json"),clock);
    assertThat(restart.status("petria",job.id()).status()).isEqualTo("UNCERTAIN");
    assertThat(restart.poll(auth.token(),List.of()).job()).isNull();
    assertThat(restart.acknowledge(auth.token(),job.id(),"SUBMITTED","Recovered local journal").status()).isEqualTo("SUBMITTED");
    restart.revoke("petria",auth.agentId());
    assertThatThrownBy(()->restart.poll(auth.token(),List.of())).hasMessageContaining("Invalid agent");
  }
  @Test void structuredDraftAndLegacyNotificationsSurviveStorage() {
    var props=new TheBotWebProperties();props.setMobileNotificationsFile(directory.resolve("mobile.json").toString());
    notifications.markRead("petria","event");
    var loaded=new MobileNotificationService(mapper,props);
    assertThat(loaded.list("petria").getFirst().replyDraft()).isEqualTo(draft);
    loaded.accept(new MobileNotificationEvent("old","petria","BOT_REPLY","title","text",null,null,null,clock.instant()));
    assertThat(loaded.list("petria").stream().filter(n->n.eventId().equals("old")).findFirst().orElseThrow().replyDraft()).isNull();
  }
  static class TestClock extends Clock {
    Instant time=Instant.parse("2026-11-12T12:00:00Z");void advance(long seconds){time=time.plusSeconds(seconds);}
    public Instant instant(){return time;}public ZoneId getZone(){return ZoneOffset.UTC;}public Clock withZone(ZoneId zone){return this;}
  }
}
