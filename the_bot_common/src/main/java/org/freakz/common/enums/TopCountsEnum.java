package org.freakz.common.enums;

import java.util.ArrayList;
import java.util.List;

public enum TopCountsEnum {

    GLUGGA_COUNT(".*(\\*glugga\\*|\\*glug\\*).*", "GLUGGA_COUNT", "%s_LAST_GLUGGA", "*glugga*", "glugga", true),
    RYYST_COUNT(".*(\\*ryyst\\*|\\*sip\\*|\\*sipasu\\*).*", "RYYST_COUNT", "%s_LAST_GLUGGA", "*ryyst*", "ryyst", true),
    PUUH_COUNT("puuh", "PUUH_COUNT", "%s_LAST_PUUH", "*puuh", "puuh", true),
    JAATAVA_COUNT(".*(jäätävä).*", "JÄÄTÄVÄ_COUNT", "%s_LAST_JÄÄTÄVÄ", "*jäätävä*", "jäätävä", true),
    KALEERI_COUNT(".*(kaleeri).*", "KALEERI_COUNT", "%s_LAST_KALEERI", "*kaleeri*", "kaleeri", true),
    KORINA_COUNT("\\s*(\\*korina*\\*|\\*kuo*len\\*|\\*tappakaa\\*).*", "KORINA_COUNT", "%s_LAST_KORINA", "*korina*", "korina", true),
    KORINATON_COUNT(".*(\\*korinaton\\*).*", "KORINATON_COUNT", "%s_LAST_KORINATON", "*korinaton*", "korinaton", true);


    private final String regex;
    private final String keyName;
    private final String lastTimeKeyName;
    private final String name;
    private final String prettyName;
    private final boolean doLastTIme;

    TopCountsEnum(String regex, String keyName, String lastTimeKeyName, String name, String prettyName, boolean doLastTime) {
        this.regex = regex;
        this.keyName = keyName;
        this.lastTimeKeyName = lastTimeKeyName;
        this.name = name;
        this.prettyName = prettyName;
        this.doLastTIme = doLastTime;
    }

    public String getRegex() {
        return regex;
    }

    public String getKeyName() {
        return keyName;
    }

    public String getLastTimeKeyName() {
        return lastTimeKeyName;
    }

    public String getName() {
        return name;
    }

    public String getPrettyName() {
        return prettyName;
    }

    public boolean isDoLastTIme() {
        return doLastTIme;
    }

    public static List<String> getPrettyNames() {
        List<String> list = new ArrayList<>();
        for (TopCountsEnum e : values()) {
            list.add(e.getPrettyName());
        }
        return list;
    }

    public static TopCountsEnum getByPrettyName(String prettyName) {
        if (prettyName == null) {
            return null;
        }
        for (TopCountsEnum e : values()) {
            if (e.getPrettyName().equals(prettyName)) {
                return e;
            }
        }
        return null;
    }

}
