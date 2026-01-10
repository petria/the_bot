package org.freakz.common.model.connectionmanager;

import java.util.List;
import java.util.Objects;

public class ChannelUsersByTargetAliasResponse {

    private List<ChannelUser> channelUsers;

    public ChannelUsersByTargetAliasResponse() {
    }

    public ChannelUsersByTargetAliasResponse(List<ChannelUser> channelUsers) {
        this.channelUsers = channelUsers;
    }

    public static Builder builder() {
        return new Builder();
    }

    public List<ChannelUser> getChannelUsers() {
        return channelUsers;
    }

    public void setChannelUsers(List<ChannelUser> channelUsers) {
        this.channelUsers = channelUsers;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ChannelUsersByTargetAliasResponse that = (ChannelUsersByTargetAliasResponse) o;
        return Objects.equals(channelUsers, that.channelUsers);
    }

    @Override
    public int hashCode() {
        return Objects.hash(channelUsers);
    }

    @Override
    public String toString() {
        return "ChannelUsersByTargetAliasResponse{" +
                "channelUsers=" + channelUsers +
                '}';
    }

    public static class Builder {
        private List<ChannelUser> channelUsers;

        public Builder channelUsers(List<ChannelUser> channelUsers) {
            this.channelUsers = channelUsers;
            return this;
        }

        public ChannelUsersByTargetAliasResponse build() {
            return new ChannelUsersByTargetAliasResponse(channelUsers);
        }
    }
}
