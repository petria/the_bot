package org.freakz.common.chat;

public class ChatIdentity {

  private final String protocol;
  private final String chatType;
  private final String chatId;
  private final String network;
  private final String target;

  public ChatIdentity(String protocol, String chatType, String chatId, String network, String target) {
    this.protocol = protocol;
    this.chatType = chatType;
    this.chatId = chatId;
    this.network = network;
    this.target = target;
  }

  public String getProtocol() {
    return protocol;
  }

  public String getChatType() {
    return chatType;
  }

  public String getChatId() {
    return chatId;
  }

  public String getNetwork() {
    return network;
  }

  public String getTarget() {
    return target;
  }
}
