package org.freakz.web.controller;

import org.freakz.web.mobile.IrssiReplyService;
import org.freakz.web.security.BotUserPrincipal;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/mobile/irssi")
public class MobileIrssiController {
  private final IrssiReplyService replies;
  public MobileIrssiController(IrssiReplyService replies){this.replies=replies;}
  @PostMapping("/pairing") public IrssiReplyService.PairCode pairing(@AuthenticationPrincipal BotUserPrincipal user){return replies.createPairing(user.getUsername(),user.getIrcNick());}
  @GetMapping("/agents") public List<IrssiReplyService.AgentView> agents(@AuthenticationPrincipal BotUserPrincipal user){return replies.agents(user.getUsername());}
  @DeleteMapping("/agents/{id}") public void revoke(@AuthenticationPrincipal BotUserPrincipal user,@PathVariable String id){replies.revoke(user.getUsername(),id);}
  @GetMapping("/drafts/{id}/availability") public IrssiReplyService.Availability available(@AuthenticationPrincipal BotUserPrincipal user,@PathVariable String id){return replies.availability(user.getUsername(),id);}
  @GetMapping("/drafts/{id}/jobs") public List<IrssiReplyService.Job> jobs(@AuthenticationPrincipal BotUserPrincipal user,@PathVariable String id){return replies.jobsForDraft(user.getUsername(),id);}
  @PostMapping("/drafts/{id}/send") public IrssiReplyService.Job send(@AuthenticationPrincipal BotUserPrincipal user,@PathVariable String id,@RequestBody Send request){return replies.submit(user.getUsername(),id,request.text(),Boolean.TRUE.equals(request.retry()));}
  @GetMapping("/jobs/{id}") public IrssiReplyService.Job status(@AuthenticationPrincipal BotUserPrincipal user,@PathVariable String id){return replies.status(user.getUsername(),id);}
  @com.fasterxml.jackson.annotation.JsonIgnoreProperties(ignoreUnknown=true)
  public record Send(String text,Boolean retry){}
}
