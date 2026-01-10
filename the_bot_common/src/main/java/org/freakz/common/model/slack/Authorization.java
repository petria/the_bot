package org.freakz.common.model.slack;

import java.util.Objects;

class Authorization {
    private String enterpriseId;
    private String teamId;
    private String userId;
    private boolean isBot;
    private boolean isEnterpriseInstall;

    public Authorization() {
    }

    public Authorization(String enterpriseId, String teamId, String userId, boolean isBot, boolean isEnterpriseInstall) {
        this.enterpriseId = enterpriseId;
        this.teamId = teamId;
        this.userId = userId;
        this.isBot = isBot;
        this.isEnterpriseInstall = isEnterpriseInstall;
    }

    public String getEnterpriseId() {
        return enterpriseId;
    }

    public void setEnterpriseId(String enterpriseId) {
        this.enterpriseId = enterpriseId;
    }

    public String getTeamId() {
        return teamId;
    }

    public void setTeamId(String teamId) {
        this.teamId = teamId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public boolean isBot() {
        return isBot;
    }

    public void setBot(boolean bot) {
        isBot = bot;
    }

    public boolean isEnterpriseInstall() {
        return isEnterpriseInstall;
    }

    public void setEnterpriseInstall(boolean enterpriseInstall) {
        isEnterpriseInstall = enterpriseInstall;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Authorization that = (Authorization) o;
        return isBot == that.isBot && isEnterpriseInstall == that.isEnterpriseInstall && Objects.equals(enterpriseId, that.enterpriseId) && Objects.equals(teamId, that.teamId) && Objects.equals(userId, that.userId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(enterpriseId, teamId, userId, isBot, isEnterpriseInstall);
    }

    @Override
    public String toString() {
        return "Authorization{" +
                "enterpriseId='" + enterpriseId + '\'' +
                ", teamId='" + teamId + '\'' +
                ", userId='" + userId + '\'' +
                ", isBot=" + isBot +
                ", isEnterpriseInstall=" + isEnterpriseInstall +
                '}';
    }
}
