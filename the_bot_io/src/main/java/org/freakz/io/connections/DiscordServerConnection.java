package org.freakz.io.connections;

import lombok.extern.slf4j.Slf4j;
import org.freakz.common.dto.BotConnection;
import org.freakz.common.dto.BotConnectionType;
import org.freakz.common.model.json.botconfig.DiscordConfig;
import org.freakz.common.model.json.feed.Message;
import org.javacord.api.DiscordApi;
import org.javacord.api.DiscordApiBuilder;
import org.javacord.api.entity.channel.Channel;
import org.javacord.api.entity.channel.PrivateChannel;
import org.javacord.api.entity.channel.ServerTextChannel;
import org.javacord.api.entity.server.Server;
import org.javacord.api.entity.user.User;
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
                .setAllIntents()
                .setToken(token).login().join();
        this.api.addMessageCreateListener(this::messageListener);
//
//        apithis.api.getChannels().stream().toList().get(0).asServerTextChannel().get().getServer().
//        String botInvite = api.createBotInvite();
//        int foo = 0;
        Set<Server> servers = api.getServers();
        Server server = servers.stream().toList().get(0);
        Set<User> members = server.getMembers();
        int foo = 0;
    }

    @Override
    public void sendMessageTo(Message message) {
        Channel channel = null;
        Set<Channel> channels = api.getChannels();
        for (Channel ch : channels) {
            if (ch.getId() == message.getId()) {
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
        Set<Channel> channels = this.api.getChannels();
        int foo = 0;
    }

}
