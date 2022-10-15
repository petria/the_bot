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


    public static class Listener {
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
            int foo = 0;
            log.debug("Got msg: {}", event.getMessage());

        }

        @Handler
        public void handleConnectionEstablished(ClientConnectionEstablishedEvent event) {
        }

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

        client.getEventManager().registerEventListener(new Listener());
        config.getChannelList().forEach(ch -> client.addChannel(ch.getName()));

    }

}
