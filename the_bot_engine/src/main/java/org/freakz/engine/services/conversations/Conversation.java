package org.freakz.engine.services.conversations;

import org.freakz.common.model.engine.EngineRequest;

public class Conversation {

  private long id;

  private ConversationType type;

  private int state = 0;

  private String trigger;

  private ConversationContent content;

  public Conversation() {
  }

  public Conversation(long id, ConversationType type, int state, String trigger, ConversationContent content) {
    this.id = id;
    this.type = type;
    this.state = state;
    this.trigger = trigger;
    this.content = content;
  }

  public long getId() {
    return id;
  }

  public void setId(long id) {
    this.id = id;
  }

  public ConversationType getType() {
    return type;
  }

  public void setType(ConversationType type) {
    this.type = type;
  }

  public int getState() {
    return state;
  }

  public void setState(int state) {
    this.state = state;
  }

  public String getTrigger() {
    return trigger;
  }

  public void setTrigger(String trigger) {
    this.trigger = trigger;
  }

  public ConversationContent getContent() {
    return content;
  }

  public void setContent(ConversationContent content) {
    this.content = content;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    Conversation that = (Conversation) o;

    if (id != that.id) return false;
    if (state != that.state) return false;
    if (type != that.type) return false;
    if (trigger != null ? !trigger.equals(that.trigger) : that.trigger != null) return false;
    return content != null ? content.equals(that.content) : that.content == null;
  }

  @Override
  public int hashCode() {
    int result = (int) (id ^ (id >>> 32));
    result = 31 * result + (type != null ? type.hashCode() : 0);
    result = 31 * result + state;
    result = 31 * result + (trigger != null ? trigger.hashCode() : 0);
    result = 31 * result + (content != null ? content.hashCode() : 0);
    return result;
  }

  @Override
  public String toString() {
    return "Conversation{" +
        "id=" + id +
        ", type=" + type +
        ", state=" + state +
        ", trigger='" + trigger + '\'' +
        ", content=" + content +
        '}';
  }

  public int nextState() {
    this.state++;
    return this.state;
  }

  public String handleConversation(EngineRequest request) {
    return this.content.handleConversation(request, state);
  }
}
