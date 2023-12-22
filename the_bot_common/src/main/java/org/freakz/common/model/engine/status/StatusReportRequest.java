package org.freakz.common.model.engine.status;

import lombok.*;

import java.util.Map;

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

    private Map<String, ChannelMessageCounters> countersMap;

}
