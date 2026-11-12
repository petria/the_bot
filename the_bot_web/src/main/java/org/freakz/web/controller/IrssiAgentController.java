package org.freakz.web.controller;

import org.freakz.web.mobile.IrssiReplyService;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import java.util.List;

@RestController
@RequestMapping("/api/irssi")
public class IrssiAgentController {
  private final IrssiReplyService replies;
  public IrssiAgentController(IrssiReplyService replies){this.replies=replies;}
  @PostMapping("/pair") public IrssiReplyService.Credentials pair(@RequestBody Pair request){return replies.pair(request.code(),request.name());}
  @PostMapping("/poll") public IrssiReplyService.Poll poll(@RequestHeader(value="Authorization",required=false) String authorization,@RequestBody Poll request){return replies.poll(token(authorization),request.connections());}
  @PostMapping("/jobs/{id}/result") public IrssiReplyService.Job result(@RequestHeader(value="Authorization",required=false) String authorization,@PathVariable String id,@RequestBody Result request){return replies.acknowledge(token(authorization),id,request.status(),request.detail());}
  private String token(String header){if(header==null||!header.startsWith("Bearer ")) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);return header.substring(7);}
  public record Pair(String code,String name){}
  public record Poll(List<IrssiReplyService.Connection> connections){}
  public record Result(String status,String detail){}
}
