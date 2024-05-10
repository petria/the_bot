package org.freakz.engine.functions;

import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;


@Slf4j
public class IrcInfoService implements Function<IrcInfoService.Request, IrcInfoService.Response> {

    public IrcInfoService() {
        log.debug("Init!");
    }

    @Override
    public Response apply(Request ircInfoRequest) {
        log.info("IrcInfo Request: {}", ircInfoRequest);
        Response response = new Response(getIrcInfo());
        log.info("IrcInfo Response: {}", response);
        return response;
    }

    private IrcInfo getIrcInfo() {
        IrcInfo ircInfo = new IrcInfo("IRCNet", getJoinedChannels());
        return ircInfo;
    }

    private JoinedChannels getJoinedChannels() {
        JoinedChannels joinedChannels = new JoinedChannels(getChannelList());
        return joinedChannels;
    }

    private List<JoinedChannel> getChannelList() {
        List<JoinedChannel> list = new ArrayList<>();
        JoinedChannel joinedChannel = new JoinedChannel("#HokanDEV2", getChannelUserList("#HokanDEV2"));
        list.add(joinedChannel);
        return list;
    }

    private List<ChannelUser> getChannelUserList(String channelName) {
        List<ChannelUser> users = new ArrayList<>();
        ChannelUser user = new ChannelUser("testNick", "Testi Niggi", "10 min");
        users.add(user);
        user = new ChannelUser("HokanDEVv", "Hokan The Bot", "0");
        users.add(user);
        return users;
    }


    public record Request(String context) {
    }

    public record Response(IrcInfo ircInfo) {
    }

    public record IrcInfo(String connectedServer, JoinedChannels joinedChannels) {
    }

    public record JoinedChannels(List<JoinedChannel> channelList) {
    }

    public record JoinedChannel(String channelName, List<ChannelUser> channelUser) {
    }

    public record ChannelUser(String nickName, String realName, String idleTime) {
    }
}
