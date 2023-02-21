package org.freakz.common.util;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Date;

/**
 * Calculates time difference between two time objects.
 * <p>
 * Date: 11/6/13
 * Time: 6:52 PM
 *
 * @author Petri Airio
 */
public class Uptime implements Serializable, Cloneable {

    private long time;

    public Uptime(long time) {
        this.time = time;
    }

    public Uptime(LocalDateTime jouluTime) {
        long epochSecond = jouluTime.toEpochSecond(ZoneOffset.ofHours(0));
        this.time = epochSecond * 1000;
    }

    @Override
    protected Object clone() throws CloneNotSupportedException {
        Uptime clone = new Uptime(this.time);
        return clone;
    }

    public Integer[] getTimeDiff() {
        return getTimeDiff(new Date().getTime());
    }

    public Integer[] getTimeDiff(long time2) {
        long utime = Math.abs(this.time - time2);

        if (utime == 0) {
            return new Integer[]{0, 0, 0, 0};
        }

        long ut_secs = utime / 1000;

        int dd = (int) ut_secs / (60 * 60 * 24);
        int hh = (int) (ut_secs / (60 * 60)) - (dd * 24);
        int mm = (int) (ut_secs / 60) - (dd * 1440) - (hh * 60);
        int ss = (int) ut_secs - (dd * 24 * 60 * 60) - (hh * 60 * 60) - (mm * 60);

        Integer[] ret = new Integer[4];
        ret[0] = ss;
        ret[1] = mm;
        ret[2] = hh;
        ret[3] = dd;

        return ret;
    }

    public String toString() {
        String[] format = {"00", "00", "00", "0"};
        StringBuilder sb = new StringBuilder();
        Integer[] ut = getTimeDiff();

        sb.append("up %3 day");
        if (ut[3] > 1) {
            sb.append("s");
        } else {
            sb.append(" ");
        }
        sb.append(" %2:%1:%0");
        return StringStuff.fillTemplate(sb.toString(), ut, format);
    }

}
