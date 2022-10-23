package org.freakz.connections;

import lombok.extern.slf4j.Slf4j;
import org.freakz.common.model.json.DiscordConfig;
import org.javacord.api.DiscordApi;
import org.javacord.api.DiscordApiBuilder;
import org.javacord.api.event.message.MessageCreateEvent;

@Slf4j
public class DiscordServerConnection extends BotConnection {

    private final EventPublisher publisher;
    private DiscordApi api;

    public DiscordServerConnection(EventPublisher publisher) {
        super(BotConnectionType.DISCORD_CONNECTION);
        this.publisher = publisher;
    }

    public void init(String botNick, DiscordConfig config) {

        String token = config.getToken();
        this.api
                = new DiscordApiBuilder()
                .setAllIntents()
                .setToken(token).login().join();
        this.api.addMessageCreateListener(this::messageListener);

//        String botInvite = api.createBotInvite();
//        int foo = 0;

    }

    private void messageListener(MessageCreateEvent event) {
        log.debug("Discord msg: {}", event.toString());
        publisher.publishEvent(this, event);

    }

}
