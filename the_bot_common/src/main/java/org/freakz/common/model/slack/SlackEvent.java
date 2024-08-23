package org.freakz.common.model.slack;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SlackEvent {
    private String token;
    private String teamId;
    private String contextTeamId;
    private String contextEnterpriseId;
    private String apiAppId;
    private Event event;
    private String type;
    private String eventId;
    private long eventTime;
    private List<Authorization> authorizations;
    private boolean isExtSharedChannel;
    private String eventContext;
}