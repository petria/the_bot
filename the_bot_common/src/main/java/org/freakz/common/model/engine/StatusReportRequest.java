package org.freakz.common.model.engine;

import lombok.*;

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

}
