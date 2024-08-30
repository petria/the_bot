package org.freakz.io.connections;


import com.slack.api.Slack;
import com.slack.api.methods.MethodsClient;
import com.slack.api.methods.request.chat.ChatPostMessageRequest;
import com.slack.api.methods.response.chat.ChatPostMessageResponse;
import lombok.extern.slf4j.Slf4j;
import org.freakz.common.model.botconfig.Channel;
import org.freakz.common.model.botconfig.SlackConfig;
import org.freakz.common.model.feed.Message;
import org.freakz.common.model.slack.SlackEvent;
import org.freakz.io.contoller.SlackEventsController;

@Slf4j
public class SlackConnection extends BotConnection {


    private SlackEventsController slackEventsController;
    private ConnectionManager connectionManager;
    private EventPublisher publisher;
    private SlackConfig slackConfig;

/*    public SlackConnection(SlackEventsController slackEventsController) {
        this.slackEventsController = slackEventsController;
    }*/

    @Override
    public BotConnectionType getType() {
        return BotConnectionType.SLACK_CONNECTION;
    }

    @Override
    public String getNetwork() {
        return "Slack";
    }

    public void init(ConnectionManager connectionManager, String botNick, SlackConfig config, SlackEventsController slackEventsController, EventPublisher eventPublisher) {
        this.connectionManager = connectionManager;
        this.publisher = eventPublisher;
        this.slackConfig = config;
        slackEventsController.init(this);
    }

    public Channel resolveSlackChannel(String slackChannelId) {
        for (Channel channel : this.slackConfig.getChannelList()) {
            if (channel.getId().equals(slackChannelId)) {
                return channel;
            }
        }
        return null;
    }

    public Channel resolveSlackChannel(SlackEvent event) {
        return resolveSlackChannel(event.getEvent().getChannel());
    }

    public void handleSlackEvent(SlackEvent event) {
        log.debug("Handle event: {}", event);
        if (event.getEvent().getUser().equals(slackConfig.getBotSlackUserId())) {
            log.debug("Ignore own message");
            return;
        }
        if (this.publisher != null) {
            Channel channel = resolveSlackChannel(event);
            if (channel != null) {
                this.publisher.publishEvent(this, event, channel.getEchoToAlias());
            } else {
                log.warn("Could not publish event to Slack, no configured channel for event!");
            }
        }
    }

    @Override
    public void sendMessageTo(Message message) {

        log.debug("Send message: {}", message);
        Channel channel = resolveSlackChannel(message.getTarget());
        if (channel != null) {

            Slack slack = Slack.getInstance();

            String token = this.slackConfig.getSlackToken();
            // Initialize an API Methods client with the given token
            MethodsClient methods = slack.methods(token);
//        methods.getSlackHttpClient().
            // Build a request object
            ChatPostMessageRequest request = ChatPostMessageRequest.builder()
//                    .channel("#the-bot")
                    .channel(channel.getName())
                    .text(message.getMessage())
                    .build();

            // Get a response as a Java object
            ChatPostMessageResponse response = null;
            try {
                response = methods.chatPostMessage(request);

                // Check if the message sent successfully
                if (response.isOk()) {
                    com.slack.api.model.Message postedMessage = response.getMessage();
                    log.info("Success! Message posted to slack: {}", postedMessage);
                } else {
                    String errorCode = response.getError(); // e.g., "invalid_auth", "channel_not_found"
                    log.error("Error while posting chat message in slack: {}", errorCode);
                }
            } catch (Exception e) {
                log.error("Error while sending a message to Slack. @error={}", e.getMessage());
            }

        } else {
            log.error("Can't find configured Channel to send Slack reply to!");
        }
    }


}
