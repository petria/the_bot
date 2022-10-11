package org.freakz.common.model.json;

import lombok.Builder;
import lombok.Data;
@Builder
@Data
public class IrcServer {

    private String host;
    private int port;


}
