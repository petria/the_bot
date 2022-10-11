package org.freakz.common.model.json;

import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class IrcNetwork {

    private String name;
    private IrcServer ircServer;
}
