package org.freakz.io.connections;


import lombok.extern.slf4j.Slf4j;
import org.freakz.common.model.botconfig.SlackConfig;
import org.freakz.common.model.slack.SlackEvent;
import org.freakz.io.contoller.SlackEventsController;

@Slf4j
public class SlackConnection extends BotConnection {


    private SlackEventsController slackEventsController;
    private ConnectionManager connectionManager;

/*    public SlackConnection(SlackEventsController slackEventsController) {
        this.slackEventsController = slackEventsController;
    }*/

    @Override
    public String getNetwork() {
        return "Slack";
    }

    public void init(ConnectionManager connectionManager, String botNick, SlackConfig config, SlackEventsController slackEventsController) {
        this.connectionManager = connectionManager;
        slackEventsController.init(this);
    }

    public void handleSlackEvent(SlackEvent event) {
        log.debug("Handle event: {}", event);
    }

}
