package org.freakz.common.model.slack;

import java.util.List;
import java.util.Objects;

public class SlackEvent {
  private String token;
  private String teamId;
  private String contextTeamId;
  private String contextEnterpriseId;
  private String apiAppId;
  private Event event;
  private String type;
  private String eventId;
  private long eventTime;
  private List<Authorization> authorizations;
  private boolean isExtSharedChannel;
  private String eventContext;

  public SlackEvent() {
  }

  public SlackEvent(String token, String teamId, String contextTeamId, String contextEnterpriseId, String apiAppId, Event event, String type, String eventId, long eventTime, List<Authorization> authorizations, boolean isExtSharedChannel, String eventContext) {
    this.token = token;
    this.teamId = teamId;
    this.contextTeamId = contextTeamId;
    this.contextEnterpriseId = contextEnterpriseId;
    this.apiAppId = apiAppId;
    this.event = event;
    this.type = type;
    this.eventId = eventId;
    this.eventTime = eventTime;
    this.authorizations = authorizations;
    this.isExtSharedChannel = isExtSharedChannel;
    this.eventContext = eventContext;
  }

  public String getToken() {
    return token;
  }

  public void setToken(String token) {
    this.token = token;
  }

  public String getTeamId() {
    return teamId;
  }

  public void setTeamId(String teamId) {
    this.teamId = teamId;
  }

  public String getContextTeamId() {
    return contextTeamId;
  }

  public void setContextTeamId(String contextTeamId) {
    this.contextTeamId = contextTeamId;
  }

  public String getContextEnterpriseId() {
    return contextEnterpriseId;
  }

  public void setContextEnterpriseId(String contextEnterpriseId) {
    this.contextEnterpriseId = contextEnterpriseId;
  }

  public String getApiAppId() {
    return apiAppId;
  }

  public void setApiAppId(String apiAppId) {
    this.apiAppId = apiAppId;
  }

  public Event getEvent() {
    return event;
  }

  public void setEvent(Event event) {
    this.event = event;
  }

  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  public String getEventId() {
    return eventId;
  }

  public void setEventId(String eventId) {
    this.eventId = eventId;
  }

  public long getEventTime() {
    return eventTime;
  }

  public void setEventTime(long eventTime) {
    this.eventTime = eventTime;
  }

  public List<Authorization> getAuthorizations() {
    return authorizations;
  }

  public void setAuthorizations(List<Authorization> authorizations) {
    this.authorizations = authorizations;
  }

  public boolean isExtSharedChannel() {
    return isExtSharedChannel;
  }

  public void setExtSharedChannel(boolean extSharedChannel) {
    isExtSharedChannel = extSharedChannel;
  }

  public String getEventContext() {
    return eventContext;
  }

  public void setEventContext(String eventContext) {
    this.eventContext = eventContext;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    SlackEvent that = (SlackEvent) o;
    return eventTime == that.eventTime && isExtSharedChannel == that.isExtSharedChannel && Objects.equals(token, that.token) && Objects.equals(teamId, that.teamId) && Objects.equals(contextTeamId, that.contextTeamId) && Objects.equals(contextEnterpriseId, that.contextEnterpriseId) && Objects.equals(apiAppId, that.apiAppId) && Objects.equals(event, that.event) && Objects.equals(type, that.type) && Objects.equals(eventId, that.eventId) && Objects.equals(authorizations, that.authorizations) && Objects.equals(eventContext, that.eventContext);
  }

  @Override
  public int hashCode() {
    return Objects.hash(token, teamId, contextTeamId, contextEnterpriseId, apiAppId, event, type, eventId, eventTime, authorizations, isExtSharedChannel, eventContext);
  }

  @Override
  public String toString() {
    return "SlackEvent{" +
        "token='" + token + '\'' +
        ", teamId='" + teamId + '\'' +
        ", contextTeamId='" + contextTeamId + '\'' +
        ", contextEnterpriseId='" + contextEnterpriseId + '\'' +
        ", apiAppId='" + apiAppId + '\'' +
        ", event=" + event +
        ", type='" + type + '\'' +
        ", eventId='" + eventId + '\'' +
        ", eventTime=" + eventTime +
        ", authorizations=" + authorizations +
        ", isExtSharedChannel=" + isExtSharedChannel +
        ", eventContext='" + eventContext + '\'' +
        '}';
  }
}
