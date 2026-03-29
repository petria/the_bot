package org.freakz.common.chat;

public class ChatIdentity {

  private  String protocol;
  private  String chatType;
  private  String chatId;
  private  String network;
  private  String target;

  public ChatIdentity() {
  }

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

  public void setProtocol(String protocol) {
    this.protocol = protocol;
  }

  public String getChatType() {
    return chatType;
  }

  public void setChatType(String chatType) {
    this.chatType = chatType;
  }

  public String getChatId() {
    return chatId;
  }

  public void setChatId(String chatId) {
    this.chatId = chatId;
  }

  public String getNetwork() {
    return network;
  }

  public void setNetwork(String network) {
    this.network = network;
  }

  public String getTarget() {
    return target;
  }

  public void setTarget(String target) {
    this.target = target;
  }
}
