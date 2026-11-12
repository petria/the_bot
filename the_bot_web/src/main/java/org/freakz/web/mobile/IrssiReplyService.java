package org.freakz.web.mobile;

import org.freakz.common.model.mobile.ReplyDraft;
import org.freakz.web.config.TheBotWebProperties;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.json.JsonMapper;
import java.nio.file.*;
import java.time.*;
import java.util.*;
import java.security.*;

/** User-approved sends only. Agent credentials authorize this queue, never mobile account APIs. */
@Service
public class IrssiReplyService {
  private final MobileNotificationService notifications;
  private final JsonMapper mapper;
  private final Path file;
  private final Clock clock;
  private final Map<String, Pairing> pairings = new HashMap<>();
  private final Map<String, Presence> presence = new HashMap<>();
  private final List<Agent> agents = new ArrayList<>();
  private final List<Job> jobs = new ArrayList<>();
  @org.springframework.beans.factory.annotation.Autowired
  public IrssiReplyService(MobileNotificationService notifications, @Qualifier("jsonMapper") JsonMapper mapper, TheBotWebProperties properties) {
    this(notifications, mapper, Path.of(properties.getMobileNotificationsFile()).resolveSibling("irssi-replies.json"), Clock.systemUTC());
  }
  IrssiReplyService(MobileNotificationService notifications, JsonMapper mapper, Path file, Clock clock) {
    this.notifications=notifications; this.mapper=mapper; this.file=file; this.clock=clock;
    if (Files.exists(file)) {
      try { var state=mapper.readValue(file.toFile(), State.class); agents.addAll(state.agents()); jobs.addAll(state.jobs()); }
      catch(Exception e) { throw new IllegalStateException("Cannot read Irssi reply state",e); }
    }
    // A restarted service cannot know whether a previously claimed job reached the IRC socket.
    boolean recovered=false;
    for(int i=0;i<jobs.size();i++) if("CLAIMED".equals(jobs.get(i).status())) { jobs.set(i,jobs.get(i).status("UNCERTAIN","Server restarted during dispatch")); recovered=true; }
    if(recovered) save();
    expire();
  }
  public synchronized PairCode createPairing(String owner, String nick) {
    if(blank(nick)) throw error(HttpStatus.CONFLICT,"Configure your IRC nick in your profile first");
    pairings.values().removeIf(p -> p.owner().equals(owner) || p.expiresAt().isBefore(now()));
    String code=random(); var expiry=now().plusSeconds(600);
    pairings.put(hash(code),new Pairing(owner,nick,expiry)); return new PairCode(code,expiry);
  }
  public synchronized Credentials pair(String code, String name) {
    Pairing pairing=pairings.remove(hash(code));
    if(pairing==null || !pairing.expiresAt().isAfter(now())) throw error(HttpStatus.UNAUTHORIZED,"Pairing code expired or invalid");
    String token=random(); String id=UUID.randomUUID().toString();
    agents.add(new Agent(id,pairing.owner(),pairing.nick(),hash(token),blank(name)?"Irssi":name.substring(0,Math.min(name.length(),100)),false)); save();
    return new Credentials(id,token,pairing.nick());
  }
  private Agent authenticate(String token) {
    String digest=hash(token);
    return agents.stream().filter(a -> !a.revoked() && MessageDigest.isEqual(a.tokenHash().getBytes(java.nio.charset.StandardCharsets.US_ASCII),digest.getBytes(java.nio.charset.StandardCharsets.US_ASCII)))
        .findFirst().orElseThrow(() -> error(HttpStatus.UNAUTHORIZED,"Invalid agent credential"));
  }
  public synchronized List<AgentView> agents(String owner) {
    expire();
    return agents.stream().filter(a -> a.owner().equals(owner) && !a.revoked()).map(a -> {
      Presence p=presence.get(a.id()); return new AgentView(a.id(),a.name(),a.nick(),p!=null && p.at().isAfter(now().minusSeconds(45)),p==null?List.of():p.connections());
    }).toList();
  }
  public synchronized void revoke(String owner,String id) {
    for(int i=0;i<agents.size();i++) if(agents.get(i).owner().equals(owner) && agents.get(i).id().equals(id)) {
      Agent a=agents.get(i); agents.set(i,new Agent(a.id(),a.owner(),a.nick(),a.tokenHash(),a.name(),true)); presence.remove(id);
      for(int j=0;j<jobs.size();j++) if(jobs.get(j).agentId().equals(id) && "QUEUED".equals(jobs.get(j).status())) jobs.set(j,jobs.get(j).status("FAILED","Agent revoked"));
      save(); return;
    }
    throw error(HttpStatus.NOT_FOUND,"Agent not found");
  }
  private ReplyDraft draft(String owner,String id) {
    return notifications.list(owner).stream().filter(n -> "RULE_MATCH".equals(n.type()) && "irc".equalsIgnoreCase(n.connectionType())).map(MobileNotificationService.NotificationRecord::replyDraft).filter(Objects::nonNull)
        .filter(d -> id.equals(d.id())).findFirst().orElseThrow(() -> error(HttpStatus.NOT_FOUND,"Draft not found"));
  }
  public synchronized Availability availability(String owner,String id) {
    ReplyDraft d=draft(owner,id); return new Availability(selectAgent(owner,d)!=null);
  }
  private Agent selectAgent(String owner, ReplyDraft d) {
    return agents.stream().filter(a -> !a.revoked() && a.owner().equals(owner) && same(a.nick(),d.expectedNick()))
        .filter(a -> { var p=presence.get(a.id()); return p!=null && p.at().isAfter(now().minusSeconds(45))
            && p.connections().stream().anyMatch(c -> same(c.network(),d.network()) && same(c.nick(),d.expectedNick())
                && c.channels()!=null && c.channels().stream().anyMatch(channel -> same(channel,d.channel()))); }).findFirst().orElse(null);
  }
  public synchronized Job submit(String owner,String draftId,String text,boolean retry) {
    expire(); ReplyDraft d=draft(owner,draftId); validate(d,text);
    Job old=jobs.stream().filter(j -> j.owner().equals(owner) && j.draftId().equals(draftId)).reduce((a,b)->b).orElse(null);
    if(old!=null && (!retry || Set.of("QUEUED","CLAIMED","SUBMITTED").contains(old.status()))) return old;
    Agent agent=selectAgent(owner,d);
    if(agent==null) throw error(HttpStatus.CONFLICT,"Irssi is offline or not connected with the expected nick and channel");
    String message=text.trim();
    if(!message.regionMatches(true,0,d.sender()+":",0,d.sender().length()+1)) message=d.sender()+": "+message;
    Job job=new Job(UUID.randomUUID().toString(),draftId,owner,agent.id(),d.network(),d.channel(),d.expectedNick(),message,"QUEUED",null,now(),now().plusSeconds(60));
    jobs.add(job); save(); return job;
  }
  static void validate(ReplyDraft d,String text) {
    if(blank(d.network()) || blank(d.expectedNick()) || d.channel()==null || !d.channel().matches("[#&][^\\s,:\\p{Cntrl}]{1,199}")
        || d.sender()==null || !d.sender().matches("[^\\s,:#&\\p{Cntrl}]{1,64}")) throw error(HttpStatus.BAD_REQUEST,"Invalid IRC source destination");
    if(blank(text) || text.length()>500 || text.chars().anyMatch(Character::isISOControl)) throw error(HttpStatus.BAD_REQUEST,"Reply must be one line of 1–500 characters");
  }
  public synchronized Job status(String owner,String id) {
    expire(); return jobs.stream().filter(j -> j.owner().equals(owner) && j.id().equals(id)).findFirst().orElseThrow(() -> error(HttpStatus.NOT_FOUND,"Send request not found"));
  }
  public synchronized List<Job> jobsForDraft(String owner,String draftId) {
    draft(owner,draftId); expire(); return jobs.stream().filter(j -> j.owner().equals(owner)&&j.draftId().equals(draftId)).toList();
  }
  public synchronized Poll poll(String token,List<Connection> connections) {
    Agent agent=authenticate(token); expire();
    var safe=connections==null?List.<Connection>of():connections.stream().filter(Objects::nonNull).limit(50)
        .map(c->new Connection(c.network(),c.nick(),c.channels()==null?List.of():c.channels().stream().filter(Objects::nonNull).limit(1000).toList())).toList();
    presence.put(agent.id(),new Presence(now(),safe));
    for(int i=0;i<jobs.size();i++) {
      Job j=jobs.get(i);
      if(j.agentId().equals(agent.id()) && "QUEUED".equals(j.status())) {
        Job claimed=j.status("CLAIMED",null); jobs.set(i,claimed); save(); return new Poll(claimed);
      }
    }
    return new Poll(null);
  }
  public synchronized Job acknowledge(String token,String id,String status,String detail) {
    Agent agent=authenticate(token); expire();
    if(status==null || !Set.of("SUBMITTED","FAILED","UNCERTAIN").contains(status)) throw error(HttpStatus.BAD_REQUEST,"Invalid send status");
    for(int i=0;i<jobs.size();i++) {
      Job j=jobs.get(i);
      if(j.id().equals(id) && j.agentId().equals(agent.id())) {
        if(!Set.of("CLAIMED","UNCERTAIN").contains(j.status())) return j;
        Job updated=j.status(status,detail==null?null:detail.substring(0,Math.min(200,detail.length()))); jobs.set(i,updated); save(); return updated;
      }
    }
    throw error(HttpStatus.NOT_FOUND,"Send request not found");
  }
  private void expire() {
    boolean changed=false;
    for(int i=0;i<jobs.size();i++) { Job j=jobs.get(i); if(!j.expiresAt().isAfter(now()) && Set.of("QUEUED","CLAIMED").contains(j.status())) {
      jobs.set(i,j.status("QUEUED".equals(j.status())?"EXPIRED":"UNCERTAIN","Dispatch deadline passed")); changed=true;
    }}
    if(changed) save();
  }
  private void save() {
    try { var parent=file.toAbsolutePath().getParent(); Files.createDirectories(parent); Path tmp=file.resolveSibling(file.getFileName()+".tmp");
      mapper.writeValue(tmp.toFile(),new State(agents,jobs)); Files.move(tmp,file,StandardCopyOption.REPLACE_EXISTING,StandardCopyOption.ATOMIC_MOVE);
    } catch(Exception e) { throw new IllegalStateException("Cannot persist Irssi reply state",e); }
  }
  private Instant now(){return clock.instant();}
  private static boolean blank(String s){return s==null||s.isBlank();}
  private static boolean same(String a,String b){return a!=null&&b!=null&&fold(a).equals(fold(b));}
  private static String fold(String s){return s.toLowerCase(Locale.ROOT);}
  private static String random(){ byte[] b=new byte[32];new SecureRandom().nextBytes(b);return Base64.getUrlEncoder().withoutPadding().encodeToString(b); }
  private static String hash(String value){if(blank(value)) throw error(HttpStatus.UNAUTHORIZED,"Credential required");try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(java.nio.charset.StandardCharsets.UTF_8)));}catch(NoSuchAlgorithmException e){throw new IllegalStateException(e);}}
  private static ResponseStatusException error(HttpStatus status,String message){return new ResponseStatusException(status,message);}
  private record Pairing(String owner,String nick,Instant expiresAt){}
  private record Presence(Instant at,List<Connection> connections){}
  public record PairCode(String code,Instant expiresAt){}
  public record Credentials(String agentId,String token,String expectedNick){}
  public record Connection(String network,String nick,List<String> channels){}
  public record Availability(boolean available){}
  public record Agent(String id,String owner,String nick,String tokenHash,String name,boolean revoked){}
  public record AgentView(String id,String name,String expectedNick,boolean online,List<Connection> connections){}
  public record Job(String id,String draftId,String owner,String agentId,String network,String channel,String expectedNick,String message,String status,String detail,Instant createdAt,Instant expiresAt){
    Job status(String s,String detail){return new Job(id,draftId,owner,agentId,network,channel,expectedNick,message,s,detail,createdAt,expiresAt);}
  }
  public record Poll(Job job){}
  private record State(List<Agent> agents,List<Job> jobs){}
}
