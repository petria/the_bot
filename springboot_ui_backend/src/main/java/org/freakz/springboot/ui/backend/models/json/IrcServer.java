package org.freakz.springboot.ui.backend.models.json;

import lombok.Builder;
import lombok.Data;
@Builder
@Data
public class IrcServer {

    private String host;
    private int port;


}
