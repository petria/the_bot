package org.freakz.cli.service;

import feign.Response;
import lombok.extern.slf4j.Slf4j;
import org.freakz.cli.clients.EngineClient;
import org.freakz.common.model.engine.StatusReportRequest;
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
    public void timer() {
        if (hostname == null) {
            try {
                InetAddress id = InetAddress.getLocalHost();
                System.out.println(id.getHostName());
                this.hostname = id.getHostName();
            } catch (Exception e) {
                //
            }
        }

        StatusReportRequest request
                = StatusReportRequest.builder()
                .uptimeStart(startup)
                .timestamp(System.currentTimeMillis())
                .name("BOT_CLI: " + hostname)
                .build();
        try {
            Response response = engineClient.handleStatusReport(request);
            if (response.status() != 200) {
                int foo = 0;
            }
        } catch (Exception e) {
//            e.printStackTrace();
            //
        }
    }

}
