package org.freakz.springboot.ui.backend.models.json;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Builder
@Data
public class IrcServerConfig {

    private String name;
    private IrcNetwork ircNetwork;
    private List<IrcChannel> channelList;

}
