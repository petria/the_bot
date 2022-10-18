package org.freakz.connections;

import lombok.extern.slf4j.Slf4j;
import net.engio.mbassy.listener.Handler;
import org.freakz.common.model.json.IrcServerConfig;
import org.kitteh.irc.client.library.Client;
import org.kitteh.irc.client.library.event.channel.ChannelJoinEvent;
import org.kitteh.irc.client.library.event.channel.ChannelMessageEvent;
import org.kitteh.irc.client.library.event.connection.ClientConnectionEstablishedEvent;

@Slf4j
public class IrcServerConnection {

    private final EventPublisher publisher;

    public IrcServerConnection(EventPublisher publisher) {
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
    public void onChannelMessageEvent(ChannelMessageEvent event) {
        log.debug("Got msg: {}", event.getMessage());
        publisher.publishEvent(this, event);
    }

    @Handler
    public void handleConnectionEstablished(ClientConnectionEstablishedEvent event) {
    }


    public void init(String botNick, IrcServerConfig config) {

        Client client
                = Client.builder()
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

}
