package org.freakz.common.model;

import java.io.Serializable;

public class TimeDifferenceData implements Serializable {

  private long[] diffs;

  public long[] getDiffs() {
    return diffs;
  }

  public void setDiffs(long[] diffs) {
    this.diffs = diffs;
  }
}
