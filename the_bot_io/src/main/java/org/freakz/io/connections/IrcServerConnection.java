package org.freakz.io.connections;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.engio.mbassy.listener.Handler;
import org.freakz.common.exception.BotIOException;
import org.freakz.common.exception.InvalidTargetAliasException;
import org.freakz.common.model.botconfig.IrcServerConfig;
import org.freakz.common.model.feed.Message;
import org.freakz.common.model.feed.MessageSource;
import org.kitteh.irc.client.library.Client;
import org.kitteh.irc.client.library.element.Channel;
import org.kitteh.irc.client.library.element.User;
import org.kitteh.irc.client.library.event.channel.*;
import org.kitteh.irc.client.library.event.connection.ClientConnectionEndedEvent;
import org.kitteh.irc.client.library.event.connection.ClientConnectionEstablishedEvent;
import org.kitteh.irc.client.library.event.user.WhoisEvent;
import org.kitteh.irc.client.library.util.Cutter;

import java.util.List;
import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

@Slf4j
public class IrcServerConnection extends BotConnection {

    private final EventPublisher publisher;
    private Client client;
    private ConnectionManager connectionManager;

    @Getter
    private IrcServerConfig config;
    private String botNick;

    public IrcServerConnection(EventPublisher publisher) {
        super(BotConnectionType.IRC_CONNECTION);
        this.publisher = publisher;
    }

    @Override
    public String getNetwork() {
        return config.getIrcNetwork().getName();
    }

    @Handler
    public void onUserJoinChannel(ChannelJoinEvent event) throws BotIOException {
        updateChannelMap(event.getChannel().getName());
        if (event.getClient().isUser(event.getUser())) { // It's me!
//            event.getChannel().sendMessage("Hello world! Kitteh's here for cuddles.");
            return;
        }
        // It's not me!
//        event.getChannel().sendMessage("Welcome, " + event.getUser().getNick() + "! :3");
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
    public void onChannelUsersUpdatedEvent(ChannelUsersUpdatedEvent event) throws BotIOException {
        String channelName = event.getChannel().getName();
        log.debug("onChannelUsersUpdatedEvent: {}", channelName);
        updateChannelMap(channelName);
        int foo = 0;
    }

    private void updateChannelMap(String channelName) throws BotIOException {

        org.freakz.common.model.botconfig.Channel channel = resolveByEchoTo(channelName);
        if (channel == null) {
            throw new BotIOException("No Channel config found with: " + channelName);
        }

        JoinedChannelContainer container = this.connectionManager.getJoinedChannelsMap().get(channel.getEchoToAlias());
        BotConnectionChannel botConnectionChannel;
        if (container == null) {
            botConnectionChannel = new BotConnectionChannel();
            botConnectionChannel.setName(channel.getName());
            botConnectionChannel.setId(channel.getId());
            botConnectionChannel.setType(getType().name());
            botConnectionChannel.setNetwork(getNetwork());
            botConnectionChannel.setEchoToAlias(channel.getEchoToAlias());

        } else {
            botConnectionChannel = container.channel;
        }

        this.connectionManager.updateJoinedChannelsMap(BotConnectionType.IRC_CONNECTION, this, botConnectionChannel);

        log.debug("Updated channel: {}", botConnectionChannel);
    }

    private org.freakz.common.model.botconfig.Channel resolveByEchoTo(String channelName) {
        for (org.freakz.common.model.botconfig.Channel channel : this.config.getChannelList()) {
            if (channel.getName().equalsIgnoreCase(channelName)) {
                return channel;
            }
        }
        return null;
    }

    @Handler
    public void onChannelMessageEvent(ChannelMessageEvent event) throws BotIOException {
        log.debug("Got msg: {}", event.getMessage());
        publisher.publishEvent(this, event);
        updateChannelMap(event.getChannel().getName());
        checkEchoTo(this.config, this.connectionManager, event.getChannel().getName(), event.getActor().getNick(), event.getMessage());
    }

    protected void checkEchoTo(IrcServerConfig config, ConnectionManager connectionManager, String channelName, String actorName, String message) {
        String name = channelName; //event.getChannel().getName();
        config.getChannelList().forEach(ch -> {
            if (ch.getName().equalsIgnoreCase(name)) {
                if (ch.getEchoToAliases() != null && ch.getEchoToAliases().size() > 0) {
                    for (String echoToAlias : ch.getEchoToAliases()) {
                        log.debug("Echo to: {}", echoToAlias);
                        try {
                            String msg = String.format("<%s@IRC>: %s", actorName, message);
                            connectionManager.sendMessageByTargetAlias(msg, echoToAlias);
                        } catch (InvalidTargetAliasException e) {
                            log.error("Can not echo message to: {}", echoToAlias);
                        }
                    }
                }
            }
        });
    }


    @Handler
    public void handleConnectionEstablished(ClientConnectionEstablishedEvent event) {
        this.connectionManager.ircConnectionEstablished(this);
// TODO        log.debug("Clear channel map to 0!");
        for (JoinedChannelContainer container : this.connectionManager.getJoinedChannelsMap().values()) {
            if (container.botConnectionType == BotConnectionType.IRC_CONNECTION) {
                this.connectionManager.getJoinedChannelsMap().remove(container.channel.getEchoToAlias());
            }
        }
    }

    @Handler
    public void handleConnectionEnded(ClientConnectionEndedEvent event) {
        log.debug(">> ENDED, shutting down this client");
        event.setAttemptReconnect(false);
        this.connectionManager.ircConnectionEnded(this);
        this.client.shutdown();
    }


    public void init(ConnectionManager connectionManager, String botNick, IrcServerConfig config) {
        this.connectionManager = connectionManager;
        this.config = config;
        this.botNick = botNick;

        client = Client.builder()
                .user("hokan")
                .nick(botNick)

                .server()
                .host(config.getIrcNetwork().getIrcServer().getHost())
                .port(config.getIrcNetwork().getIrcServer().getPort(), Client.Builder.Server.SecurityType.INSECURE)
                .then()
                .buildAndConnect();

        client.getEventManager().registerEventListener(this);

        config.getChannelList().forEach(ch -> {
                    if (ch.getJoinOnStart()) {
                        log.debug("Join channel: {}", ch.getName());
                        client.addChannel(ch.getName());
                    } else {
                        log.debug("Not join channel: {}", ch.getName());
                    }
                }
        );

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
                    publisher.logMessage(MessageSource.IRC_MESSAGE, getNetwork(), message.getTarget(), botNick, splitLine);
                    if (!message.getMessage().startsWith("\u0002" + "\u0002")) {
                        checkEchoTo(this.config, this.connectionManager, message.getTarget(), botNick, splitLine);
                    }
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
    }

    private final Queue<WhoisEvent> whoisEventQueue = new ConcurrentLinkedQueue<>();

    @Handler
    public void handleWhoisReply(WhoisEvent event) {
        log.debug("whois - {}", event);
        synchronized (whoisEventQueue) {
            whoisEventQueue.add(event);
            whoisEventQueue.notify();
        }
    }

    public WhoisEvent sendSyncWhois(String whois, long maxWaitTimeout) throws InterruptedException {
        client.sendRawLineImmediately(whois);
        synchronized (whoisEventQueue) {
            whoisEventQueue.wait(maxWaitTimeout);

            WhoisEvent whoisEvent = whoisEventQueue.peek();
            log.debug("Got event from queue: {}", whoisEvent);
            return whoisEvent;
        }

    }


}
