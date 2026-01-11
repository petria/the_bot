package org.freakz.common.model.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.LocalDateTime;
import java.util.Objects;

@JsonIgnoreProperties(ignoreUnknown = true)
public class DataContainerBase {

  protected final LocalDateTime lastSaved;
  protected Integer saveTimes = 0;

  public DataContainerBase(LocalDateTime lastSaved, Integer saveTimes) {
    this.lastSaved = lastSaved;
    this.saveTimes = saveTimes;
  }

  public DataContainerBase() {
    lastSaved = LocalDateTime.now();
  }

  public LocalDateTime getLastSaved() {
    return lastSaved;
  }

  public Integer getSaveTimes() {
    return saveTimes;
  }

  public void setSaveTimes(Integer saveTimes) {
    this.saveTimes = saveTimes;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    DataContainerBase that = (DataContainerBase) o;
    return Objects.equals(lastSaved, that.lastSaved) && Objects.equals(saveTimes, that.saveTimes);
  }

  @Override
  public int hashCode() {
    return Objects.hash(lastSaved, saveTimes);
  }

  @Override
  public String toString() {
    return "DataContainerBase{" +
        "lastSaved=" + lastSaved +
        ", saveTimes=" + saveTimes +
        '}';
  }
}
