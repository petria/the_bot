package org.freakz.io.connections;

import lombok.extern.slf4j.Slf4j;
import net.engio.mbassy.listener.Handler;
import org.freakz.common.model.json.IrcServerConfig;
import org.freakz.common.model.json.feed.Message;
import org.kitteh.irc.client.library.Client;
import org.kitteh.irc.client.library.element.Channel;
import org.kitteh.irc.client.library.element.User;
import org.kitteh.irc.client.library.event.channel.ChannelJoinEvent;
import org.kitteh.irc.client.library.event.channel.ChannelKickEvent;
import org.kitteh.irc.client.library.event.channel.ChannelMessageEvent;
import org.kitteh.irc.client.library.event.channel.ChannelPartEvent;
import org.kitteh.irc.client.library.event.channel.ChannelUsersUpdatedEvent;
import org.kitteh.irc.client.library.event.connection.ClientConnectionEstablishedEvent;

import java.util.Optional;
import java.util.Set;

@Slf4j
public class IrcServerConnection extends BotConnection {

    private final EventPublisher publisher;
    private Client client;

    public IrcServerConnection(EventPublisher publisher) {
        super(BotConnectionType.IRC_CONNECTION);
        this.publisher = publisher;
    }

    @Handler
    public void onUserJoinChannel(ChannelJoinEvent event) {
        if (event.getClient().isUser(event.getUser())) { // It's me!
            event.getChannel().sendMessage("Hello world! Kitteh's here for cuddles.");
            return;
        }
        // It's not me!
        event.getChannel().sendMessage("Welcome, " + event.getUser().getNick() + "! :3");
    }

    @Handler
    public void onChannelPartEvent(ChannelPartEvent event) {
        User user = event.getUser();
        if (event.getClient().isUser(event.getUser())) {
            log.debug("Parted: {}", event);
        }
    }

    @Handler
    public void onChannelKickEvent(ChannelKickEvent event) {
        int foo = 0;
    }

    @Handler
    public void onChannelUsersUpdatedEvent(ChannelUsersUpdatedEvent event) {
        int foo = 0;
    }

    @Handler
    public void onChannelMessageEvent(ChannelMessageEvent event) {
        log.debug("Got msg: {}", event.getMessage());
        publisher.publishEvent(this, event);
    }

    @Handler
    public void handleConnectionEstablished(ClientConnectionEstablishedEvent event) {
        int foo = 0;
    }


    public void init(String botNick, IrcServerConfig config) {

        client = Client.builder()
                .user("hokan")
                .nick(botNick)
                .server()
                .host(config.getIrcNetwork().getIrcServer().getHost())
                .port(config.getIrcNetwork().getIrcServer().getPort(), Client.Builder.Server.SecurityType.INSECURE)
                .then()
                .buildAndConnect();

        client.getEventManager().registerEventListener(this);
        config.getChannelList().forEach(ch -> client.addChannel(ch.getName()));

    }

    @Override
    public void sendMessageTo(Message message) {
        Optional<Channel> channel = client.getChannel(message.getTarget());
        if (channel.isPresent()) {
            channel.get().sendMessage(message.getMessage());
        } else {
            log.error("Can't send message to: {}", message.getTarget());
        }
    }

    @Override
    public void sendRawMessage(Message message) {
        log.debug("Send raw message: '{}'", message.getMessage());
        client.sendRawLineImmediately(message.getMessage());
        Set<Channel> channels = client.getChannels();
        int foo = 0;
    }

}
