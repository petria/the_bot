package org.freakz.io.connections;

import lombok.extern.slf4j.Slf4j;
import org.freakz.common.exception.BotIOException;
import org.freakz.common.exception.InvalidTargetAliasException;
import org.freakz.common.model.botconfig.DiscordConfig;
import org.freakz.common.model.feed.Message;
import org.javacord.api.DiscordApi;
import org.javacord.api.DiscordApiBuilder;
import org.javacord.api.entity.channel.Channel;
import org.javacord.api.entity.channel.PrivateChannel;
import org.javacord.api.entity.channel.ServerTextChannel;
import org.javacord.api.entity.message.MessageAttachment;
import org.javacord.api.entity.message.MessageAuthor;
import org.javacord.api.entity.server.Server;
import org.javacord.api.event.message.MessageCreateEvent;

import java.util.Optional;
import java.util.Set;

@Slf4j
public class DiscordServerConnection extends BotConnection {

    private final EventPublisher publisher;
    private DiscordApi api;
    private ConnectionManager connectionManager;
    private DiscordConfig config;

    public DiscordServerConnection(EventPublisher publisher) {
        super(BotConnectionType.DISCORD_CONNECTION);
        this.publisher = publisher;
    }

    @Override
    public String getNetwork() {
        return "Discord";
    }

    public void init(ConnectionManager connectionManager, DiscordConfig config) {

        this.connectionManager = connectionManager;
        this.config = config;

        String token = config.getToken();
        this.api = new DiscordApiBuilder()
                .addMessageCreateListener(this::messageListener)
                .addServerBecomesAvailableListener(event -> {
                    log.debug("loaded: {}", event);
                    try {
                        updateChannelMap(event.getApi());
                    } catch (BotIOException e) {
                        throw new RuntimeException(e);
                    }
                })
                .setAllIntents()
                .setToken(token)
                .setWaitForServersOnStartup(false)
                .login()
                .join();

    }

    private void updateChannelMap(DiscordApi api) throws BotIOException {
        Set<Server> servers = api.getServers();
        for (Server server : servers) {
            for (Channel channel : server.getChannels()) {

                org.freakz.common.model.botconfig.Channel ch = resolveByEchoTo(channel.getId());
                if (ch == null) {
                    log.error("No Channel config found with: " + channel);
                    continue;
                }

                JoinedChannelContainer container = this.connectionManager.getJoinedChannelsMap().get(ch.getEchoToAlias());
                BotConnectionChannel botConnectionChannel;
                if (container == null) {
                    botConnectionChannel = new BotConnectionChannel();
                    botConnectionChannel.setName(ch.getName());
                    botConnectionChannel.setId(String.valueOf(channel.getId()));
                    botConnectionChannel.setType(getType().name());
                    botConnectionChannel.setNetwork(getNetwork());
                    botConnectionChannel.setEchoToAlias(ch.getEchoToAlias());

                } else {
                    botConnectionChannel = container.channel;
                }
                this.connectionManager.updateJoinedChannelsMap(BotConnectionType.DISCORD_CONNECTION, this, botConnectionChannel);
                log.debug("Updated channel: {}", botConnectionChannel);
            }
        }

    }

    private org.freakz.common.model.botconfig.Channel resolveByEchoTo(long id) {
        for (org.freakz.common.model.botconfig.Channel channel : this.config.getChannelList()) {
            if (channel.getId().equals("" + id)) {
                return channel;
            }
        }
        return null;
    }

    @Override
    public void sendMessageTo(Message message) {
        Channel channel = null;
        Set<Channel> channels = api.getChannels();
        for (Channel ch : channels) {
            if (ch.asVoiceChannel().isPresent()) {
                continue; // Skip voice channels;
            }
            String chId = "" + ch.getId();
            if (chId.equals(message.getId())) {
                channel = ch;
                break;
            }
            String t = ch.toString();
            if (t.contains("name: " + message.getTarget())) {
                channel = ch;
                break;
            }
        }
        if (channel != null) {
            String boxed = String.format("```%s```", message.getMessage());
            Optional<ServerTextChannel> serverTextChannel = channel.asServerTextChannel();
            if (serverTextChannel.isPresent()) {
                this.connectionManager.addMessageInOut(getType().toString(), 0, 1);
                serverTextChannel.get().sendMessage(boxed);
                return;
            }
            Optional<PrivateChannel> privateChannel = channel.asPrivateChannel();
            if (privateChannel.isPresent()) {
                this.connectionManager.addMessageInOut(getType().toString(), 0, 1);
                privateChannel.get().sendMessage(boxed);
                return;
            }
            log.error("Could not send reply: {}", message);
        } else {
            log.error("Can't send message to: {}", message.getTarget());
        }

    }

    private void messageListener(MessageCreateEvent event) {
        log.debug("Discord msg: {}", event.toString());
        publisher.publishEvent(this, event);

        try {
            this.connectionManager.addMessageInOut(getType().toString(), 1, 0);
            updateChannelMap(event.getApi());
        } catch (BotIOException e) {
            throw new RuntimeException(e);
        }

        String channelStr = event.getChannel().toString();
        // "ServerTextChannel (id: 1033431599708123278, name: hokandev)"
        int idx1 = channelStr.indexOf("name: ");
        String channelName = channelStr.substring(idx1 + 6, channelStr.length() - 1);
        log.debug("replyTo: '{}'", channelName);

        MessageAuthor messageAuthor = event.getMessageAuthor();
        if (messageAuthor.asUser().isPresent()) {
            if (messageAuthor.asUser().get().getId() != this.config.getTheBotUserId()) { // dont echo back own messages
                StringBuilder messageTxt = new StringBuilder(event.getMessage().getContent());
                if (!event.getMessage().getAttachments().isEmpty()) {
                    for (MessageAttachment attachment : event.getMessageAttachments()) {
                        messageTxt.append(" [");
                        messageTxt.append(attachment.getUrl().toString());
                        messageTxt.append("]");
                    }
                }
                checkEchoTo(this.config, this.connectionManager, channelName, event.getMessageAuthor().getName(), messageTxt.toString());
            }
        }
    }

    protected void checkEchoTo(DiscordConfig config, ConnectionManager connectionManager, String channelName, String actorName, String message) {
        String name = channelName; //event.getChannel().getName();
        config.getChannelList().forEach(ch -> {
            if (ch.getName().equals(name)) {
                if (ch.getEchoToAliases() != null && ch.getEchoToAliases().size() > 0) {
                    for (String echoToAlias : ch.getEchoToAliases()) {
                        log.debug("Echo to: {}", echoToAlias);
                        try {
                            if (!message.startsWith("!")) {
                                //                                    String msg = String.format("%s%s<%s@Telegram>: %s", "\u0002", "\u0002", actorName, message);
                                String msg = String.format("<%s@Dicord>: %s", actorName, message);
                                connectionManager.sendMessageByTargetAlias(msg, echoToAlias);
                            }
                        } catch (InvalidTargetAliasException e) {
                            log.error("Can not echo message to: {}", echoToAlias);
                        }
                    }
                }
            }
        });
    }


}
