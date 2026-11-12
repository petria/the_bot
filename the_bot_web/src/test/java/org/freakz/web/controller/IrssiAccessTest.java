package org.freakz.web.controller;

import org.freakz.web.mobile.*;
import org.freakz.web.security.*;
import io.micrometer.core.instrument.MeterRegistry;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import java.util.List;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest({MobileIrssiController.class,IrssiAgentController.class})
@Import({SecurityConfig.class,MobileBearerAuthenticationFilter.class})
class IrssiAccessTest {
  @Autowired MockMvc mvc;
  @MockitoBean IrssiReplyService replies;
  @MockitoBean MobileAuthService auth;
  @MockitoBean WebLoginFailureHandler failures;
  @MockitoBean MeterRegistry registry;
  @Test void agentCredentialCannotAccessMobileAccountAndMissingAgentCredentialIsDenied() throws Exception {
    mvc.perform(get("/api/mobile/irssi/agents")).andExpect(status().isUnauthorized());
    mvc.perform(get("/api/mobile/irssi/agents").header("Authorization","Bearer agent-token")).andExpect(status().isUnauthorized());
    mvc.perform(post("/api/irssi/poll").contentType("application/json").content("{\"connections\":[]}")).andExpect(status().isUnauthorized());
    mvc.perform(post("/api/irssi/poll").with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user("web-user"))
        .contentType("application/json").content("{\"connections\":[]}")).andExpect(status().isUnauthorized());
    when(replies.poll(eq("invalid"),any())).thenThrow(new ResponseStatusException(HttpStatus.UNAUTHORIZED));
    mvc.perform(post("/api/irssi/poll").header("Authorization","Bearer invalid").contentType("application/json").content("{\"connections\":[]}")).andExpect(status().isUnauthorized());
    verify(replies,never()).agents(any());
  }
  @Test void authenticatedOwnerSubmitsOnlyEditedTextNotClientSuppliedDestination() throws Exception {
    var principal=new BotUserPrincipal(1L,"petria","unused","Petri",null,"_Pete_",null,null,null,List.of(),List.of());
    when(auth.authenticateAccessToken("mobile-token")).thenReturn(principal);
    mvc.perform(post("/api/mobile/irssi/drafts/draft-id/send").header("Authorization","Bearer mobile-token")
        .contentType("application/json").content("{\"text\":\"Edited reply\"}"))
        .andExpect(status().isOk());
    verify(replies).submit("petria","draft-id","Edited reply",false);
    mvc.perform(post("/api/mobile/irssi/drafts/draft-id/send").header("Authorization","Bearer mobile-token")
        .contentType("application/json").content("{\"text\":\"Tamper\",\"channel\":\"#wrong\",\"username\":\"victim\"}"))
        .andExpect(status().isOk());
    verify(replies).submit("petria","draft-id","Tamper",false);
    verify(replies,never()).submit(eq("victim"),any(),any(),anyBoolean());
  }
  @Test void agentPollingUsesItsOwnCredentialAndInvalidPairingFails() throws Exception {
    when(replies.poll(eq("agent-token"),any())).thenReturn(new IrssiReplyService.Poll(null));
    mvc.perform(post("/api/irssi/poll").header("Authorization","Bearer agent-token").contentType("application/json").content("{\"connections\":[]}"))
        .andExpect(status().isOk());
    when(replies.pair("bad-code","host")).thenThrow(new ResponseStatusException(HttpStatus.UNAUTHORIZED));
    mvc.perform(post("/api/irssi/pair").contentType("application/json").content("{\"code\":\"bad-code\",\"name\":\"host\"}"))
        .andExpect(status().isUnauthorized());
  }
}
