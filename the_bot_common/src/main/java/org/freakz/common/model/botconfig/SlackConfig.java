package org.freakz.common.model.botconfig;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

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
