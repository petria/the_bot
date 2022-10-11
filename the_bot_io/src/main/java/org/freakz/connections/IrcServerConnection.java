package org.freakz.connections;

import lombok.extern.slf4j.Slf4j;
import net.engio.mbassy.listener.Handler;
import org.kitteh.irc.client.library.Client;
import org.kitteh.irc.client.library.event.channel.ChannelJoinEvent;
import org.kitteh.irc.client.library.event.channel.ChannelMessageEvent;
import org.kitteh.irc.client.library.event.connection.ClientConnectionEstablishedEvent;
import org.kitteh.irc.client.library.exception.KittehNagException;

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
        }

        @Handler
        public void handleConnectionEstablished(ClientConnectionEstablishedEvent event) {
            int bar = 0;
        }

    }

    public void init() {
        try {
            Client client
                    = Client.builder()
                    .user("hokan")
                    .nick("HokanDEV")
                    .server()
                    .host("irc.stealth.net")
                    .port(6667, Client.Builder.Server.SecurityType.INSECURE)
                    .then()
                    .buildAndConnect();

            client.getEventManager().registerEventListener(new Listener());
            client.addChannel("#HokanDEV");

        } catch (KittehNagException nag) {
            log.debug("got nagged!");
        }
    }

}
