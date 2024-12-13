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
public class SlackConfig {

  private String slackToken;

  private String botSlackUserId;

  private List<Channel> channelList;

  private boolean connectStartup;
}
