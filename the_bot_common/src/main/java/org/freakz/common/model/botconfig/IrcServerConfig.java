package org.freakz.common.model.botconfig;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@Builder
@Data
@NoArgsConstructor
public class IrcServerConfig {

  private String name;
  private IrcNetwork ircNetwork;
  private List<Channel> channelList;

  private boolean connectStartup;
}
