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

    public AliveReportService(EngineClient engineClient) {
        this.engineClient = engineClient;
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

        StatusReportRequest request
                = StatusReportRequest.builder()
                .uptimeStart(startup)
                .timestamp(System.currentTimeMillis())
                .name("BOT_IO")
                .hostname(hostname)
                .user(user)
                .build();
        try {
            Response response = engineClient.handleStatusReport(request);
            if (response.status() != 200) {
                log.error("Update status failed: {}", response);
            }
        } catch (Exception e) {
            log.error("Update status failed", e);
        }
    }

}
