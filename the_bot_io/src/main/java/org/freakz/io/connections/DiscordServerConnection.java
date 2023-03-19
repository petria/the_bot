package org.freakz.io.connections;

import lombok.extern.slf4j.Slf4j;
import org.freakz.common.model.json.botconfig.DiscordConfig;
import org.freakz.common.model.json.feed.Message;
import org.javacord.api.DiscordApi;
import org.javacord.api.DiscordApiBuilder;
import org.javacord.api.entity.channel.Channel;
import org.javacord.api.entity.channel.PrivateChannel;
import org.javacord.api.entity.channel.ServerTextChannel;
import org.javacord.api.entity.server.Server;
import org.javacord.api.event.message.MessageCreateEvent;

import java.util.Optional;
import java.util.Set;

@Slf4j
public class DiscordServerConnection extends BotConnection {

    private final EventPublisher publisher;
    private DiscordApi api;

    public DiscordServerConnection(EventPublisher publisher) {
        super(BotConnectionType.DISCORD_CONNECTION);
        this.publisher = publisher;
    }

    @Override
    public String getNetwork() {
        return "Discord";
    }

    public void init(String botNick, DiscordConfig config) {

        String token = config.getToken();
        this.api
                = new DiscordApiBuilder()
                .addMessageCreateListener(this::messageListener)
                .addServerBecomesAvailableListener(event -> {
                    log.debug("loaded: {}", event);
                    updateChannelMap(event.getApi());
                })
                .setAllIntents()
                .setToken(token)
                .setWaitForServersOnStartup(false)
                .login()
                .join();

    }

    private void updateChannelMap(DiscordApi api) {
        Set<Server> servers = api.getServers();
        for (Server server : servers) {
            for (Channel channel : server.getChannels()) {
                String channelStr = channel.toString();
                int idx1 = channelStr.indexOf("name: ");
                String name = channelStr.substring(idx1 + 6, channelStr.length() - 1);

                BotConnectionChannel botConnectionChannel = getChannelMap().get(name);
                if (botConnectionChannel == null) {
                    botConnectionChannel = new BotConnectionChannel();
                    getChannelMap().put(name, botConnectionChannel);
                }
                botConnectionChannel.setId("" + channel.getId());
                botConnectionChannel.setName(name);
                botConnectionChannel.setNetwork(getNetwork());
                botConnectionChannel.setType(getType().toString());

            }
        }

    }

    @Override
    public void sendMessageTo(Message message) {
        Channel channel = null;
        Set<Channel> channels = api.getChannels();
        for (Channel ch : channels) {
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
            log.error("Could not send reply: {}" ,message);
        } else {
            log.error("Can't send message to: {}", message.getTarget());
        }

    }

    private void messageListener(MessageCreateEvent event) {
        log.debug("Discord msg: {}", event.toString());
        publisher.publishEvent(this, event);
        updateChannelMap(event.getApi());
    }

}
