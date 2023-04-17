package org.freakz.io.connections;

import lombok.extern.slf4j.Slf4j;
import org.freakz.common.exception.BotIOException;
import org.freakz.common.exception.InvalidTargetAliasException;
import org.freakz.common.model.json.botconfig.DiscordConfig;
import org.freakz.common.model.json.feed.Message;
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
                String channelStr = channel.toString();
                int idx1 = channelStr.indexOf("name: ");
                String name = channelStr.substring(idx1 + 6, channelStr.length() - 1);

                org.freakz.common.model.json.botconfig.Channel ch = resolveByEchoTo(channel.getId());
                if (ch == null) {
                    log.error("No Channel config found with: " + channel);
                    continue;
                }

                ConnectionManager.JoinedChannelContainer container = this.connectionManager.getJoinedChannelsMap().get(ch.getEchoToAlias());
                BotConnectionChannel botConnectionChannel;
                if (container == null) {
                    botConnectionChannel = new BotConnectionChannel();

                    botConnectionChannel.setId(String.valueOf(channel.getId()));
                    botConnectionChannel.setType(getType().name());
                    botConnectionChannel.setNetwork(getNetwork());

                } else {
                    botConnectionChannel = container.channel;
                }
                this.connectionManager.updateJoinedChannelsMap(BotConnectionType.DISCORD_CONNECTION, this, botConnectionChannel);
                log.debug("Updated channel: {}", botConnectionChannel);
            }
        }

    }

    private org.freakz.common.model.json.botconfig.Channel resolveByEchoTo(long id) {
        for (org.freakz.common.model.json.botconfig.Channel channel : this.config.getChannelList()) {
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

            Optional<ServerTextChannel> serverTextChannel = channel.asServerTextChannel();
            if (serverTextChannel.isPresent()) {
                serverTextChannel.get().sendMessage(message.getMessage());
                return;
            }
            Optional<PrivateChannel> privateChannel = channel.asPrivateChannel();
            if (privateChannel.isPresent()) {
                privateChannel.get().sendMessage(message.getMessage());
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
                if (event.getMessage().getAttachments().size() > 0) {
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
                                String msg = String.format("<Dicord@%s: %s>", actorName, message);
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
