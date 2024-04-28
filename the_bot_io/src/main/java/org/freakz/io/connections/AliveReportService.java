package org.freakz.io.connections;

import feign.Response;
import lombok.extern.slf4j.Slf4j;
import org.freakz.common.model.engine.status.StatusReportRequest;
import org.freakz.io.clients.EngineClient;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.net.InetAddress;

@Service
@Slf4j
public class AliveReportService {

    private final static long startup = System.currentTimeMillis();

    private final EngineClient engineClient;

    private final ConnectionManager connectionManager;

    public AliveReportService(EngineClient engineClient, ConnectionManager connectionManager) {
        this.engineClient = engineClient;
        this.connectionManager = connectionManager;
    }

    private String hostname = null;

    @Scheduled(fixedRate = 2000L)
    public void sendReport() {
        String user = "<the_bot>";
        if (hostname == null) {
            try {
                InetAddress id = InetAddress.getLocalHost();
                this.hostname = id.getHostName();
                log.debug("Got hostname: {}", this.hostname);
            } catch (Exception e) {
                this.hostname = "<resolve failed>";
            }
        }

        CallCountInterceptor callCountInterceptor = connectionManager.getCallCountInterceptor();

        StatusReportRequest request
                = StatusReportRequest.builder()
                .uptimeStart(startup)
                .timestamp(System.currentTimeMillis())
                .name("BOT_IO")
                .hostname(hostname)
                .user(user)
                .httpMethodCallMap(callCountInterceptor.getCallCounts())
                .channelMessageCountersMap(connectionManager.getCountersMap())
                .build();
        try {
            Response response = engineClient.handleStatusReport(request);
            if (response.status() != 200) {
                log.error("Update status failed: {}", response);
            }
        } catch (Exception e) {
            int foo = 0;
            //log.error("Update status failed", e);
        }
    }

}
