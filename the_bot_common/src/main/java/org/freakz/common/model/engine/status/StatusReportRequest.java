package org.freakz.common.model.engine.status;

import lombok.*;

import java.util.Map;
import java.util.concurrent.ConcurrentMap;

@AllArgsConstructor
@Builder
@Data
@NoArgsConstructor
@ToString
public class StatusReportRequest {

    private long timestamp;
    private long uptimeStart;
    private String name;
    private String hostname;
    private String user;

    private ConcurrentMap<String, Integer> httpMethodCallMap;
    private Map<String, ChannelMessageCounters> channelMessageCountersMap;

}
