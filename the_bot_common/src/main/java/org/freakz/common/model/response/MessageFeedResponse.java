package org.freakz.common.model.response;

import org.freakz.common.model.feed.Message;

import java.util.List;
import java.util.Objects;

public class MessageFeedResponse {

    private List<Message> messages;

    public MessageFeedResponse() {
    }

    public MessageFeedResponse(List<Message> messages) {
        this.messages = messages;
    }

    public static Builder builder() {
        return new Builder();
    }

    public List<Message> getMessages() {
        return messages;
    }

    public void setMessages(List<Message> messages) {
        this.messages = messages;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MessageFeedResponse that = (MessageFeedResponse) o;
        return Objects.equals(messages, that.messages);
    }

    @Override
    public int hashCode() {
        return Objects.hash(messages);
    }

    @Override
    public String toString() {
        return "MessageFeedResponse{" +
                "messages=" + messages +
                '}';
    }

    public static class Builder {
        private List<Message> messages;

        public Builder messages(List<Message> messages) {
            this.messages = messages;
            return this;
        }

        public MessageFeedResponse build() {
            return new MessageFeedResponse(messages);
        }
    }
}
