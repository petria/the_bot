package org.freakz.cli.service;

import feign.Response;
import lombok.extern.slf4j.Slf4j;
import org.freakz.cli.clients.EngineClient;
import org.freakz.common.model.engine.status.StatusReportRequest;
import org.springframework.stereotype.Service;

import java.net.InetAddress;

@Service
@Slf4j
public class AliveReportService {

    private final static long startup = System.currentTimeMillis();

    private final EngineClient engineClient;

    private final CallCountInterceptor callCountInterceptor;

    public AliveReportService(EngineClient engineClient, CallCountInterceptor callCountInterceptor) {
        this.engineClient = engineClient;
        this.callCountInterceptor = callCountInterceptor;
    }

    private String hostname = null;


    public void sendReport(String user) {
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
                .name("BOT_CLI")
                .hostname(hostname)
                .httpMethodCallMap(callCountInterceptor.getCallCounts())
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
