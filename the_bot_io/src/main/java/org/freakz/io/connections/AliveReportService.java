package org.freakz.io.connections;

import feign.Response;
import lombok.extern.slf4j.Slf4j;
import org.freakz.common.model.engine.StatusReportRequest;
import org.freakz.io.clients.EngineClient;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class AliveReportService {

    private final static long startup = System.currentTimeMillis();

    private final EngineClient engineClient;

    public AliveReportService(EngineClient engineClient) {
        this.engineClient = engineClient;
    }

    @Scheduled(fixedRate = 2000L)
    public void timer() {
        StatusReportRequest request
                = StatusReportRequest.builder()
                .uptimeStart(startup)
                .timestamp(System.currentTimeMillis())
                .name("BOT_IO")
                .build();
        try {
            Response response = engineClient.handleStatusReport(request);
            if (response.status() != 200) {
                int foo = 0;
            }
        } catch (Exception e) {
            e.printStackTrace();
            //
        }
    }

}
