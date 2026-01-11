package org.freakz.engine.dto.stats;

import java.time.LocalDate;

public class StatsNode {
  public String nick;
  public int totalDays;
  public int statsDays;

  public int notStatsDays;
  public Double statDaysPercent;

  public LocalDate plusStreakStart = null;
  public LocalDate plusStreakEnd = null;

  public int plusStreakDays = 0;

  public LocalDate minusStreakStart = null;
  public LocalDate minusStreakEnd = null;
  public int minusStreakDays = 0;

  public LocalDate firstStatDay;
  public LocalDate lastStatDay;

  public StatsNode() {
  }

  public String getNick() {
    return nick;
  }

  public void setNick(String nick) {
    this.nick = nick;
  }

  public int getTotalDays() {
    return totalDays;
  }

  public void setTotalDays(int totalDays) {
    this.totalDays = totalDays;
  }

  public int getStatsDays() {
    return statsDays;
  }

  public void setStatsDays(int statsDays) {
    this.statsDays = statsDays;
  }

  public int getNotStatsDays() {
    return notStatsDays;
  }

  public void setNotStatsDays(int notStatsDays) {
    this.notStatsDays = notStatsDays;
  }

  public Double getStatDaysPercent() {
    return statDaysPercent;
  }

  public void setStatDaysPercent(Double statDaysPercent) {
    this.statDaysPercent = statDaysPercent;
  }

  public LocalDate getPlusStreakStart() {
    return plusStreakStart;
  }

  public void setPlusStreakStart(LocalDate plusStreakStart) {
    this.plusStreakStart = plusStreakStart;
  }

  public LocalDate getPlusStreakEnd() {
    return plusStreakEnd;
  }

  public void setPlusStreakEnd(LocalDate plusStreakEnd) {
    this.plusStreakEnd = plusStreakEnd;
  }

  public int getPlusStreakDays() {
    return plusStreakDays;
  }

  public void setPlusStreakDays(int plusStreakDays) {
    this.plusStreakDays = plusStreakDays;
  }

  public LocalDate getMinusStreakStart() {
    return minusStreakStart;
  }

  public void setMinusStreakStart(LocalDate minusStreakStart) {
    this.minusStreakStart = minusStreakStart;
  }

  public LocalDate getMinusStreakEnd() {
    return minusStreakEnd;
  }

  public void setMinusStreakEnd(LocalDate minusStreakEnd) {
    this.minusStreakEnd = minusStreakEnd;
  }

  public int getMinusStreakDays() {
    return minusStreakDays;
  }

  public void setMinusStreakDays(int minusStreakDays) {
    this.minusStreakDays = minusStreakDays;
  }

  public LocalDate getFirstStatDay() {
    return firstStatDay;
  }

  public void setFirstStatDay(LocalDate firstStatDay) {
    this.firstStatDay = firstStatDay;
  }

  public LocalDate getLastStatDay() {
    return lastStatDay;
  }

  public void setLastStatDay(LocalDate lastStatDay) {
    this.lastStatDay = lastStatDay;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    StatsNode statsNode = (StatsNode) o;

    if (totalDays != statsNode.totalDays) return false;
    if (statsDays != statsNode.statsDays) return false;
    if (notStatsDays != statsNode.notStatsDays) return false;
    if (plusStreakDays != statsNode.plusStreakDays) return false;
    if (minusStreakDays != statsNode.minusStreakDays) return false;
    if (nick != null ? !nick.equals(statsNode.nick) : statsNode.nick != null) return false;
    if (statDaysPercent != null ? !statDaysPercent.equals(statsNode.statDaysPercent) : statsNode.statDaysPercent != null)
      return false;
    if (plusStreakStart != null ? !plusStreakStart.equals(statsNode.plusStreakStart) : statsNode.plusStreakStart != null)
      return false;
    if (plusStreakEnd != null ? !plusStreakEnd.equals(statsNode.plusStreakEnd) : statsNode.plusStreakEnd != null)
      return false;
    if (minusStreakStart != null ? !minusStreakStart.equals(statsNode.minusStreakStart) : statsNode.minusStreakStart != null)
      return false;
    if (minusStreakEnd != null ? !minusStreakEnd.equals(statsNode.minusStreakEnd) : statsNode.minusStreakEnd != null)
      return false;
    if (firstStatDay != null ? !firstStatDay.equals(statsNode.firstStatDay) : statsNode.firstStatDay != null)
      return false;
    return lastStatDay != null ? lastStatDay.equals(statsNode.lastStatDay) : statsNode.lastStatDay == null;
  }

  @Override
  public int hashCode() {
    int result = nick != null ? nick.hashCode() : 0;
    result = 31 * result + totalDays;
    result = 31 * result + statsDays;
    result = 31 * result + notStatsDays;
    result = 31 * result + (statDaysPercent != null ? statDaysPercent.hashCode() : 0);
    result = 31 * result + (plusStreakStart != null ? plusStreakStart.hashCode() : 0);
    result = 31 * result + (plusStreakEnd != null ? plusStreakEnd.hashCode() : 0);
    result = 31 * result + plusStreakDays;
    result = 31 * result + (minusStreakStart != null ? minusStreakStart.hashCode() : 0);
    result = 31 * result + (minusStreakEnd != null ? minusStreakEnd.hashCode() : 0);
    result = 31 * result + minusStreakDays;
    result = 31 * result + (firstStatDay != null ? firstStatDay.hashCode() : 0);
    result = 31 * result + (lastStatDay != null ? lastStatDay.hashCode() : 0);
    return result;
  }

  @Override
  public String toString() {
    return "StatsNode{" +
        "nick='" + nick + '\'' +
        ", totalDays=" + totalDays +
        ", statsDays=" + statsDays +
        ", notStatsDays=" + notStatsDays +
        ", statDaysPercent=" + statDaysPercent +
        ", plusStreakStart=" + plusStreakStart +
        ", plusStreakEnd=" + plusStreakEnd +
        ", plusStreakDays=" + plusStreakDays +
        ", minusStreakStart=" + minusStreakStart +
        ", minusStreakEnd=" + minusStreakEnd +
        ", minusStreakDays=" + minusStreakDays +
        ", firstStatDay=" + firstStatDay +
        ", lastStatDay=" + lastStatDay +
        '}';
  }
}
