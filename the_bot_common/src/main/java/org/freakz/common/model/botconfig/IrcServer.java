package org.freakz.common.model.botconfig;

import java.util.Objects;

public class IrcServer {

    private String host;
    private int port;

    public IrcServer() {
    }

    public IrcServer(String host, int port) {
        this.host = host;
        this.port = port;
    }

    public static Builder builder() {
        return new Builder();
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        IrcServer ircServer = (IrcServer) o;
        return port == ircServer.port && Objects.equals(host, ircServer.host);
    }

    @Override
    public int hashCode() {
        return Objects.hash(host, port);
    }

    @Override
    public String toString() {
        return "IrcServer{" +
                "host='" + host + '\'' +
                ", port=" + port +
                '}';
    }

    public static class Builder {
        private String host;
        private int port;

        public Builder host(String host) {
            this.host = host;
            return this;
        }

        public Builder port(int port) {
            this.port = port;
            return this;
        }

        public IrcServer build() {
            return new IrcServer(host, port);
        }
    }
}
