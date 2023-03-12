package org.freakz.common.model.json.botconfig;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

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
