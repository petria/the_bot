package org.freakz.common.model.slack;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Event {
  private String user;
  private String type;
  private String ts;
  private String clientMsgId;
  private String text;
  private String team;
  private List<Block> blocks;
  private String channel;
  private String eventTs;
  private String channelType;
}
