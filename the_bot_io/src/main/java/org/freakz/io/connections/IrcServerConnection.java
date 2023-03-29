package org.freakz.io.connections;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.engio.mbassy.listener.Handler;
import org.freakz.common.model.json.botconfig.IrcServerConfig;
import org.freakz.common.model.json.feed.Message;
import org.kitteh.irc.client.library.Client;
import org.kitteh.irc.client.library.element.Channel;
import org.kitteh.irc.client.library.element.User;
import org.kitteh.irc.client.library.event.channel.ChannelJoinEvent;
import org.kitteh.irc.client.library.event.channel.ChannelKickEvent;
import org.kitteh.irc.client.library.event.channel.ChannelMessageEvent;
import org.kitteh.irc.client.library.event.channel.ChannelPartEvent;
import org.kitteh.irc.client.library.event.channel.ChannelUsersUpdatedEvent;
import org.kitteh.irc.client.library.event.connection.ClientConnectionEndedEvent;
import org.kitteh.irc.client.library.event.connection.ClientConnectionEstablishedEvent;
import org.kitteh.irc.client.library.util.Cutter;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Slf4j
public class IrcServerConnection extends BotConnection {

    private final EventPublisher publisher;
    private Client client;
    private ConnectionManager connectionManager;

    @Getter
    private IrcServerConfig config;

    public IrcServerConnection(EventPublisher publisher) {
        super(BotConnectionType.IRC_CONNECTION);
        this.publisher = publisher;
    }

    @Override
    public String getNetwork() {
        return config.getIrcNetwork().getName();
    }

    @Handler
    public void onUserJoinChannel(ChannelJoinEvent event) {
        if (event.getClient().isUser(event.getUser())) { // It's me!
            event.getChannel().sendMessage("Hello world! Kitteh's here for cuddles.");
            return;
        }
        // It's not me!
        event.getChannel().sendMessage("Welcome, " + event.getUser().getNick() + "! :3");
        updateChannelMap(event.getChannel().getName());
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
        String channelName = event.getChannel().getName();
        log.debug("onChannelUsersUpdatedEvent: {}", channelName);
        updateChannelMap(channelName);
        int foo = 0;
    }

    private void updateChannelMap(String channelName) {
        BotConnectionChannel botConnectionChannel = getChannelMap().get(channelName);
        if (botConnectionChannel == null) {
            botConnectionChannel = new BotConnectionChannel();
            botConnectionChannel.setId("" + getChannelMap().size());

            botConnectionChannel.setTargetAlias("IRC-" + getChannelMap().size());
            getChannelMap().put(channelName, botConnectionChannel);

        }
        botConnectionChannel.setName(channelName);
        botConnectionChannel.setType(getType().name());
        botConnectionChannel.setNetwork(getNetwork());

        log.debug("Updated channel: {}", botConnectionChannel);
    }

    @Handler
    public void onChannelMessageEvent(ChannelMessageEvent event) {
        log.debug("Got msg: {}", event.getMessage());
        publisher.publishEvent(this, event);
        updateChannelMap(event.getChannel().getName());
    }

    @Handler
    public void handleConnectionEstablished(ClientConnectionEstablishedEvent event) {
        this.connectionManager.ircConnectionEstablished(this);
        log.debug("Clear channel map to 0!");
        getChannelMap().clear();
    }

    @Handler
    public void handleConnectionEnded(ClientConnectionEndedEvent event) {
        log.debug(">> ENDED, shutting down this client");
        event.setAttemptReconnect(false);
        this.connectionManager.ircConnectionEnded(this);
        this.client.shutdown();
    }

//    @Handler
//    public void handleConnectionClosed(ClientConnectionClosedEvent event) {
//        log.debug(">> CLOSED");
//    }

    public void init(ConnectionManager connectionManager, String botNick, IrcServerConfig config) {
        this.connectionManager = connectionManager;
        this.config = config;

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
            Cutter messageCutter = client.getMessageCutter();
            List<String> split = messageCutter.split(message.getMessage(), 400);
            for (String line : split) {
                String splitted[] = line.split("\n");
                for (String splitLine : splitted) {
                    channel.get().sendMessage(splitLine);
                }
            }
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
