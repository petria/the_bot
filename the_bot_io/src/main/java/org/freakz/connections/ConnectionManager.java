package org.freakz.connections;


import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;

@Service
@Slf4j
public class ConnectionManager {




    @PostConstruct
    public void init() {
        log.debug(">> Start IrcServerConnections");
        IrcServerConnection isc = new IrcServerConnection();
        isc.init();
        log.debug("<< done!");
    }


}
