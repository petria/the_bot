package org.freakz.common.model.botconfig;

import java.util.Objects;

public class IrcNetwork {

    private String name;
    private IrcServer ircServer;

    public IrcNetwork() {
    }

    public IrcNetwork(String name, IrcServer ircServer) {
        this.name = name;
        this.ircServer = ircServer;
    }

    public static Builder builder() {
        return new Builder();
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public IrcServer getIrcServer() {
        return ircServer;
    }

    public void setIrcServer(IrcServer ircServer) {
        this.ircServer = ircServer;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        IrcNetwork that = (IrcNetwork) o;
        return Objects.equals(name, that.name) && Objects.equals(ircServer, that.ircServer);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, ircServer);
    }

    @Override
    public String toString() {
        return "IrcNetwork{" +
                "name='" + name + '\'' +
                ", ircServer=" + ircServer +
                '}';
    }

    public static class Builder {
        private String name;
        private IrcServer ircServer;

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder ircServer(IrcServer ircServer) {
            this.ircServer = ircServer;
            return this;
        }

        public IrcNetwork build() {
            return new IrcNetwork(name, ircServer);
        }
    }
}
