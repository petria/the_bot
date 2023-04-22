package org.freakz.common.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.security.MessageDigest;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;


/**
 * String manipulation methods.
 * <p>
 * Date: 11/6/13
 * Time: 6:52 PM
 *
 * @author Petri Airio <petri.j.airio@gmail.com>
 */
public class StringStuff {

    private static final Logger log = LoggerFactory.getLogger(StringStuff.class);

    public final static SimpleDateFormat STRING_STUFF_DF_HH = new SimpleDateFormat("HH");
    public final static SimpleDateFormat STRING_STUFF_DF_HHMM = new SimpleDateFormat("HH:mm");
    public final static SimpleDateFormat STRING_STUFF_DF_HHMMSS = new SimpleDateFormat("HH:mm:ss");
    public final static SimpleDateFormat STRING_STUFF_DF_DDMMHHMM = new SimpleDateFormat("dd.MM. HH:mm");
    public final static SimpleDateFormat STRING_STUFF_DF_DDMMYYYYHHMM = new SimpleDateFormat("dd.MM.yyyy HH:mm");
    public final static SimpleDateFormat STRING_STUFF_DF_DDMMYYYY = new SimpleDateFormat("dd.MM.yyyy");
    public final static SimpleDateFormat STRING_STUFF_DF_DMM = new SimpleDateFormat("d.MM.");
    public final static SimpleDateFormat STRING_STUFF_DF_DM = new SimpleDateFormat("d.M.");
    private final static String[] entityTable = {
            "&Auml;", "Ä",
            "%C3%84", "Ä",

            "&auml;", "ä",
            "%C3%A4", "ä",

            "&Ouml;", "Ö",
            "&ouml;", "ö",
            "%C3%B6", "ö",

            "&Aring;", "Å",
            "&aring;", "å",

            "&nbsp;", " ",
            "&mdash;", "-",
            "&quot;", "\"",
            "&raquo;", ">>",
            "&bull;", "o",
            "&amp;", "&",
            "&#x22;", " ",
            "&ndash;", "-",
            "&#x202a;", "",
            "&#x202c;", "",
            "&rlm;", ""
    };
    private static Random random = new Random(new Date().getTime());
    private static Map<String, StringBuilder> quoteCache = new HashMap<>();
    private static char PASSWD_CHARS[]
            = {'A', 'a', 'B', 'C', 'D', 'X', 'y', '4', '5', '6', '7', 'k', 'm', 'Q', 's'};
    private static String HAXOR_STRING_CONV_TABLE[] = {
            "rules", "rul3z",
            "skills", "skillz",
            "cause", "cuz",
            "because", "cuz",
            "rocks", "rockz",
            "is", "iz",
            "have", "gotz",
            "beats", "b3atz",
            // Don't forget about the quakers
            "frags", "fragz",
            "fear", "phear",
            "elite", "l33t",
            "you", "u",
            "hello", "yo",
            "a", "@",
            "A", "@",
            "e", "3",
            "E", "3",
            "o", "0",
            "O", "0",
            "s", "5",
            "S", "$",
            "t", "7",
            "T", "7"};

    private StringStuff() {
    }

    public static boolean match(String s, String pat) {
        return match(s, pat, true);
    }

    // . ^ $ ? * + | { } [ ] :

    public static boolean match(String s, String pat, boolean ignoreCase) {
        if (s == null || pat == null) {
            return false;
        }
        Pattern p;
        try {
            if (ignoreCase) {
                p = Pattern.compile(pat, Pattern.CASE_INSENSITIVE);
            } else {
                p = Pattern.compile(pat);
            }
        } catch (PatternSyntaxException ex) {
            log.error("Match gone wrong!", ex);
            return false;
        }
        Matcher matcher = p.matcher(s);
        return matcher.matches();
    }

    public static String joinStringArray(Object[] array, int fromPos) {
        StringBuilder sb = new StringBuilder();
        for (int xx = fromPos; xx < array.length; xx++) {
            if (xx != fromPos) {
                sb.append(" ");
            }
            String str = "" + array[xx];
            sb.append(str.trim());
        }
        return sb.toString();
    }

    public static String fillTemplate(String template, Object[] values) {
        return fillTemplate(template, values, null);
    }

    public static String fillTemplate(String template, Object[] values,
                                      String[] format) {

        if (template.contains("%@")) {
            String join = joinStringArray(values, 0);
            template = template.replaceAll("%@", join);
        }

        int xx;
        for (xx = 0; xx < values.length; xx++) {
            String str = "%" + xx;
            String rplc;
            if (format != null && format[xx] != null) {
                DecimalFormat df = new DecimalFormat(format[xx]);
                rplc = df.format(values[xx]);

            } else {
                rplc = values[xx] + "";
            }
            template = template.replaceAll(str, rplc);
        }

        return template;
    }

    public static String unQuote(String l) {
        l = l.replaceAll("\\.", ".");
        l = l.replaceAll("\\^", "^");
        l = l.replaceAll("\\$", "$");
        l = l.replaceAll("\\?", "?");
        l = l.replaceAll("\\*", "*");
        l = l.replaceAll("\\+", "+");
        l = l.replaceAll("\\|", "|");
        l = l.replaceAll("\\[", "[");
        l = l.replaceAll("\\]", "]");
        return l;
    }

    public static String quoteRegExp(String l) {
        StringBuilder sb = quoteCache.get(l);
        if (sb != null) {
            return sb.toString();
        }
        sb = new StringBuilder();
//    LOG.info("ORIG: " + l);
        for (int xx = 0; xx < l.length(); xx++) {
            char ch = l.charAt(xx);
            if (ch == '.') {
                sb.append("\\");
            } else if (ch == '^') {
                sb.append("\\");
            } else if (ch == '$') {
                sb.append("\\");
            } else if (ch == '?') {
                sb.append("\\");
            } else if (ch == '*') {
                sb.append("\\");
            } else if (ch == '+') {
                sb.append("\\");
            } else if (ch == '|') {
                sb.append("\\");
            } else if (ch == '[') {
                sb.append("\\");
            } else if (ch == ']') {
                sb.append("\\");
            } else if (ch == '(') {
                sb.append("\\");
            } else if (ch == ')') {
                sb.append("\\");
            } else if (ch == '{') {
                sb.append("\\");
            }
            sb.append(ch);
        }
        quoteCache.put(l, sb);
        return sb.toString();
    }

    public static String wildCardsToRegExp(String str) {
        StringBuilder sb = new StringBuilder();
        if (str.length() == 0) {
            return sb.toString();
        }
        sb.append(".*(");
        for (int xx = 0; xx < str.length(); xx++) {
            char ch = str.charAt(xx);
            if (ch == '*') {
                sb.append(".*");
            } else if (ch == '?') {
                sb.append(".");
            } else {
                sb.append(ch);
            }
        }
        sb.append(").*");
        //    LOG.info("CONVERTED PATTERN: " + sb.toString());
        return sb.toString();
    }

    public static String formatTime(Date d) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(d);
        return formatTime(cal);
    }

    public static String formatTime(Calendar cal) {

        int day1 = Calendar.getInstance().get(Calendar.DAY_OF_MONTH);
        int day2 = cal.get(Calendar.DAY_OF_MONTH);

        String dayStr;
        if (day1 == day2) {
            dayStr = STRING_STUFF_DF_HHMM.format(cal.getTime());
        } else {
            int year1 = Calendar.getInstance().get(Calendar.YEAR);
            int year2 = cal.get(Calendar.YEAR);
            if (year1 != year2) {
                dayStr = STRING_STUFF_DF_DDMMYYYYHHMM.format(cal.getTime());
            } else {
                dayStr = STRING_STUFF_DF_DDMMHHMM.format(cal.getTime());
            }

        }
        return dayStr;
    }

    /**
     * Common date parsing method to be used to convert String to the date. This currently
     * expects the dateStr be in format dd.MM.yyyy e.g.  09.06.1972 / 24.12.2005
     *
     * @param dateStr the String to be converted to the Date object
     * @param sdf     formatter to be used for parsing
     * @return the parsed Date object or null if parsing failed
     */
    public static Date parseStringToDate(String dateStr, SimpleDateFormat sdf) {
        Date date;
        if (dateStr.matches(("\\d\\d\\.\\d\\d\\."))) {
            Calendar cal = Calendar.getInstance();
            String yearStr = "" + cal.get(Calendar.YEAR);
            dateStr = dateStr + yearStr;
            log.info("Yearified dateStr = " + dateStr);
        }
        try {
            date = STRING_STUFF_DF_DDMMYYYY.parse(dateStr);
        } catch (ParseException e) {
            log.error("Invalid dateStr: " + dateStr);
            return null;
        }
        return date;
    }

    public static String formatTime(Date date, SimpleDateFormat sdf) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        return sdf.format(cal.getTime());
    }

    public static boolean isDateToday(Date date) {
        String todayStr = formatTime(new Date(), STRING_STUFF_DF_DDMMYYYY);
        String compareStr = formatTime(date, STRING_STUFF_DF_DDMMYYYY);
        return todayStr.equals(compareStr);
    }

    public static String formatNiceDate(Date date, boolean minutes, boolean seconds) {
        if (date == null) {
            return "<?>";
        }

        if (isDateToday(date)) {
            if (seconds) {
                return formatTime(date, STRING_STUFF_DF_HHMMSS);
            } else {
                return formatTime(date, STRING_STUFF_DF_HHMM);
            }
        } else {
            return formatTime(date, STRING_STUFF_DF_DDMMHHMM);
        }
    }

    public static String formatNiceDate(Date date, boolean seconds) {
        if (date == null) {
            return "<?>";
        }
        if (isDateToday(date)) {
            if (seconds) {
                return formatTime(date, STRING_STUFF_DF_HHMMSS);
            } else {
                return formatTime(date, STRING_STUFF_DF_HHMM);
            }
        } else {
            return formatTime(date, STRING_STUFF_DF_DDMMHHMM);
        }
    }

    /**
     * Simple string crypting routine.
     *
     * @param str the string to be crypted
     * @param key the key used to crypt
     * @return the crypted string
     */
    public static String xorString(String str, String key) {

        byte[] bytes1 = str.getBytes();
        byte[] bytes2 = key.getBytes();
        byte[] res = new byte[str.length()];
        int ptr = key.length() - 1;
        for (int i = 0; i < str.length(); i++) {
            res[i] = (byte) (bytes1[i] ^ bytes2[ptr--]);
            if (ptr == -1) {
                ptr = key.length() - 1;
            }
//      System.out.println(i + " -> " + res[i]);
        }
        return new String(res);
    }

    /**
     * Generates random passwd from the defined set of characters.
     *
     * @param length - the maxium lenght of the generated passwd
     * @return generated passwd
     */
    public static String generatePasswd(int length) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length; i++) {
            int rnd = random.nextInt(PASSWD_CHARS.length - 1);
            sb.append(PASSWD_CHARS[rnd]);
        }
        return sb.toString();
    }

    public static String haxorizeString(String str) {
        if (str != null && str.length() > 0) {
            for (int i = 0; i < HAXOR_STRING_CONV_TABLE.length; i = i + 2) {
                try {
                    str = str.replaceAll(HAXOR_STRING_CONV_TABLE[i], HAXOR_STRING_CONV_TABLE[i + 1]);
                } catch (Exception ex) {
                    log.error("FUCK!");
                }
            }
        } else {
            str = "Haz0rZNullz";
        }
        return str;
    }

    public static String htmlEntitiesToText(String entityString) {

        for (int i = 0; i < entityTable.length; i += 2) {
            entityString = entityString.replaceAll(entityTable[i], entityTable[i + 1]);
        }
        entityString = entityString.replaceAll("&#\\d*;", "");
        return entityString;
    }

    public static String htmlEntityDecode(String s) {

        int i, j, pos = 0;
        StringBuilder sb = new StringBuilder();
        while ((i = s.indexOf("&#", pos)) != -1 && (j = s.indexOf(';', i)) != -1) {
            int n = -1;
            for (i += 2; i < j; ++i) {
                char c = s.charAt(i);
                if ('0' <= c && c <= '9')
                    n = (n == -1 ? 0 : n * 10) + c - '0';
                else
                    break;
            }
            if (i != j) n = -1;      // malformed entity - abort
            if (n != -1) {
                sb.append((char) n);
                i = j + 1;      // skip ';'
            } else {
                for (int k = pos; k < i; ++k)
                    sb.append(s.charAt(k));
            }
            pos = i;
        }
        if (sb.length() == 0)
            return s;
        else
            sb.append(s.substring(pos, s.length()));
        return sb.toString();

    }


    /**
     * @param str String to be capitelized
     * @return the Capitalized String
     */
    public static String capitalize(String str) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < str.length(); i++) {
            char ch = str.charAt(i);
            if (i == 0) {
                sb.append(Character.toUpperCase(ch));
            } else {
                sb.append(Character.toLowerCase(ch));
            }
        }
        return sb.toString();
    }

    /**
     * Resolves ip-address to hostname
     *
     * @param str containg ip-address
     * @return the hostname of ip-address
     */
    public static String resolveIP(String str) {
        StringBuilder sb = new StringBuilder();
        if (str.length() == 9) {
            str = str.substring(1);
        }
        byte[] ip = new byte[4];
        for (int i = 0; i < 4; i++) {
            if (i != 0) {
                sb.append(".");
            }
            int xx = Integer.parseInt(str.substring((i * 2), (i * 2) + 2), 16);
            ip[i] = (byte) xx;
            sb.append(xx);
        }
        sb.append(" (");
        try {
            InetAddress ia = InetAddress.getByAddress(ip);
            sb.append(ia.getHostName());
        } catch (UnknownHostException e) {
            sb.append("<unknown>");
        }
        sb.append(")");
        return sb.toString();
    }

    public static String randomizeString(String s) {
        List<Character> chars = new ArrayList<>();
        for (int i = 0; i < s.length(); i++) {
            chars.add(s.charAt(i));
        }
        Collections.shuffle(chars);
        StringBuilder sb = new StringBuilder();
        for (Character aChar : chars) {
            sb.append(aChar);
        }

        return sb.toString();
    }

    /**
     * @param data    Object to format in column
     * @param colSize column size in chars
     * @param padding what to use as padding character
     * @return formatted column
     */
    public static String formatColumn(Object data, int colSize, char padding) {
        StringBuilder column = new StringBuilder();
        column.append(data);
        while (column.length() != colSize) {
            column.insert(0, padding);
        }
        return column.toString();
    }

    public static int checkOccurences(String str, String[] toFind) {
        int score = 0;
        int pos = 0;
        //    LOG.info("-- - -- > CHECK START");
        for (String aToFind : toFind) {
            do {
                pos = str.indexOf(aToFind, pos);
                if (pos != -1) {
                    pos += aToFind.length();
                    score++;
                }
            } while (pos != -1);
        }
        //    LOG.info("-- - -- > CHECK END");
        return score;
    }

    public static boolean parseBooleanString(String string) {
        return match(string, "true|1|yes|on", true);
    }


    public static String getRandomString(String... msgs) {
        int idx = random.nextInt(msgs.length);
        return msgs[idx];
    }

    /**
     * This method ensures that the output String has only valid XML unicode characters as specified by the
     * <p>
     * XML 1.0 standard. For reference, please see the
     * <p>
     * standard. This method will return an empty String if the input is null or empty.
     * Author Donoiu Cristian, GPL
     *
     * @param s The String whose non-valid characters we want to remove.
     * @return The in String, stripped of non-valid characters.
     */

    public static String removeInvalidXMLCharacters(String s) {

        StringBuilder out = new StringBuilder();                // Used to hold the output.

        int codePoint;                                          // Used to reference the current character.

        //String ss = "\ud801\udc00";                           // This is actualy one unicode character, represented by two code units!!!.

        //System.out.println(ss.codePointCount(0, ss.length()));// See: 1

        int i = 0;

        while (i < s.length()) {

//      System.out.println("i=" + i);

            codePoint = s.codePointAt(i);                       // This is the unicode code of the character.

            if ((codePoint == 0x9) ||                      // Consider testing larger ranges first to improve speed.

                    (codePoint == 0xA) ||

                    (codePoint == 0xD) ||

                    ((codePoint >= 0x20) && (codePoint <= 0xD7FF)) ||

                    ((codePoint >= 0xE000) && (codePoint <= 0xFFFD)) ||

                    ((codePoint >= 0x10000) && (codePoint <= 0x10FFFF))) {

                out.append(Character.toChars(codePoint));

            }

            i += Character.charCount(codePoint);                 // Increment with the number of code units(java chars) needed to represent a Unicode char.

        }
        return out.toString();
    }

    public static boolean isInBetween(String srcStr, String toFind, char limiter) {

        int idx = srcStr.indexOf(toFind);
        if (idx == -1) {
            return false;
        }

        boolean leftBoundary = false;
        boolean rightBoundary = false;

        int tmp = idx;
        while (tmp >= 0) {
            char c = srcStr.charAt(tmp);
            if (c == limiter) {
                leftBoundary = true;
                break;
            }
            tmp--;
        }

        if (!leftBoundary) {
            return false;
        }

        tmp = idx;
        while (tmp <= srcStr.length()) {
            char c = srcStr.charAt(tmp);
            if (c == limiter) {
                rightBoundary = true;
                break;
            }
            tmp++;
        }

        return rightBoundary;
    }

    public static String arrayToString(Object[] array, String delim) {
        StringBuilder sb = new StringBuilder();
        for (Object obj : array) {
            if (sb.length() > 0) {
                sb.append(delim);
            }
            sb.append(obj);
        }
        return sb.toString();
    }

    public static String getSHA1Password(String password) {
        try {
            MessageDigest messageDigest = MessageDigest.getInstance("SHA-1");
            byte[] bytes = messageDigest.digest(password.getBytes());
            StringBuilder hexString = new StringBuilder();

            for (byte aByte : bytes) {
                String val = Integer.toHexString(0xFF & aByte);
                if (val.length() == 1) {
                    hexString.append("0");
                }
                hexString.append(val);
            }
            return hexString.toString();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
