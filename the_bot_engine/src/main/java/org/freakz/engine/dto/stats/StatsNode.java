package org.freakz.engine.dto.stats;

import lombok.Data;

import java.time.LocalDate;

@Data
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
}
