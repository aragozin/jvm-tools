package org.gridkit.jvmtool.stacktrace.analytics;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TimeRangeChecker {

    private static String DATE_FORMAT = "yyyy.MM.dd_HH:mm:ss";

    private static String DATE_PATTERN = "(((((((\\d\\d)?\\d\\d[.])?\\d\\d[.])?\\d\\d[_])?\\d\\d[:])?\\d\\d[:])\\d\\d)";

    private long[] whiteCache = new long[0];
    private long[] blackCache = new long[0];
    private int[] lowerBound;
    private int[] upperBound;
    private TimeZone tz;
    private boolean inverted = false;

    public TimeRangeChecker(String lower, String upper, TimeZone tz) {
        lowerBound = parse(lower);
        upperBound = parse(upper);
        this.tz = tz;
        if (tz == null) {
            throw new NullPointerException("tz is null");
        }

        for(int i = 0; i != lowerBound.length; ++i) {
            if ((lowerBound[i] == -1 && upperBound[i] != -1) || (lowerBound[i] != -1 && upperBound[i] == -1)) {
                throw new IllegalArgumentException("Bounds length mismatch '" + lower + "', '" + upper + "'");
            }
            else {
                if (lowerBound[i] == upperBound[i]) {
                    continue;
                }
                if (lowerBound[i] > upperBound[i]) {
                    inverted = true;
                }
                break;
            }
        }
    }

    private int[] parse(String bound) {
        try {
            Matcher m = Pattern.compile(DATE_PATTERN).matcher(bound);
            if (!m.matches()) {
                throw new IllegalArgumentException("Cannot parser time range bound '" + bound + "'");
            }
            int[] p = new int[7];
            Arrays.fill(p, -1);
            if (m.group(7) != null) {
                p[0] = Integer.parseInt(m.group(7));
            }
            if (m.group(6) != null) {
                p[1] = Integer.parseInt(substring(m.group(6), 2, 1));
            }
            if (m.group(5) != null) {
                p[2] = Integer.parseInt(substring(m.group(5), 2, 1));
            }
            if (m.group(4) != null) {
                p[3] = Integer.parseInt(substring(m.group(4), 2, 1));
            }
            if (m.group(3) != null) {
                p[4] = Integer.parseInt(substring(m.group(3), 2, 1));
            }
            if (m.group(2) != null) {
                p[5] = Integer.parseInt(substring(m.group(2), 2, 1));
            }
            if (m.group(1) != null) {
                p[6] = Integer.parseInt(substring(m.group(1), 2, 0));
            }
            return p;
        }
        catch(NumberFormatException e) {
            throw new IllegalArgumentException("Cannot parser time range bound '" + bound + "'");
        }
    }

    private String substring(String txt, int n, int m) {
        return txt.substring(txt.length() - n -m, txt.length() - m);
    }

    // Exposed for testing
    boolean isCached(long timestamp) {
        return match(whiteCache, timestamp) || match(blackCache, timestamp);
    }

    public boolean evaluate(long timestamp) {
        if (match(whiteCache, timestamp)) {
            return true;
        }
        if (match(blackCache, timestamp)) {
            return false;
        }

        SimpleDateFormat fmt = new SimpleDateFormat(DATE_FORMAT);
        fmt.setTimeZone(tz);
        String date = fmt.format(timestamp);

        int[] p = parse(date);
        int[] rl = Arrays.copyOf(p, p.length);
        int[] ru = Arrays.copyOf(p, p.length);

        boolean match = match(p, lowerBound, upperBound);

        for(int j = 0; j != rl.length; ++j) {
            if (lowerBound[j] != -1) {
                rl[j] = lowerBound[j];
                ru[j] = upperBound[j];
            }
        }
        if (match) {
            // match
            long ldate = toDate(rl);
            long udate = toDate(ru);
            if (ldate == -1 || udate == -1) {
                // do not cache
                return true;
            }
            if (udate > ldate) {
                whiteCache = add(whiteCache, ldate, udate);
            }
            else {
                if (timestamp >= ldate && timestamp >= udate) {
                    udate = adjust(udate, 1);
                    whiteCache = add(whiteCache, ldate, udate);
                }
                else {
                    ldate = adjust(ldate, -1);
                    whiteCache = add(whiteCache, ldate, udate);
                }
            }
            return true;
        }
        else {
            // do not match
            long ldate = toDate(rl);
            long udate = toDate(ru);
            if (ldate == -1 || udate == -1) {
                // do not cache
                return false;
            }
            if (timestamp >= ldate && timestamp >= udate) {
                ldate = adjust(ldate, 1);
                blackCache = add(blackCache, udate, ldate);
            }
            else {
                udate = adjust(udate, -1);
                blackCache = add(blackCache, udate, ldate);
            }
            return false;
        }
    }

    private boolean match(int[] p, int[] lower, int[] upper) {
        boolean matchUpper = true;
        boolean matchLower = true;
        for(int i = 0; i != lowerBound.length; ++i) {
            if (lowerBound[i] != -1) {
                if (checkInRange(p, i, matchLower, matchUpper)) {
                    if (upperBound[i] != p[i]) {
                        matchUpper = false;
                    }
                    if (lowerBound[i] != p[i]) {
                        matchLower = false;
                    }
                    if (!matchLower && !matchUpper) {
                        return true;
                    }
                    continue;
                }
                else {
                    return false;
                }
            }
        }
        return !matchUpper;
    }

    private boolean checkInRange(int[] p, int i, boolean matchLower, boolean matchUpper) {
        if (inverted) {
            return (((matchLower) && lowerBound[i] <= p[i]) || ((matchUpper) && upperBound[i] >= p[i]));
        }
        else {
            return ((!matchLower) || lowerBound[i] <= p[i]) && ((!matchUpper) || upperBound[i] >= p[i]);
        }
    }

    private static int[] FIELDS = {Calendar.YEAR, Calendar.YEAR, Calendar.MONTH, Calendar.DAY_OF_MONTH, Calendar.HOUR_OF_DAY, Calendar.MINUTE, Calendar.SECOND};

    private long adjust(long ldate, int step) {
        Calendar cal = Calendar.getInstance(tz);
        cal.setTime(new Date(ldate));
        int n = -1;
        for(int i = 0; i != lowerBound.length; ++i) {
            if (lowerBound[i] == -1) {
                n = i;
            }
        }
        if (n == -1) {
            cal.add(Calendar.YEAR, 100 * step);
        }
        else if (n == 0) {
            cal.add(Calendar.YEAR, 10 * step);
        }
        else {
            cal.add(FIELDS[n], step);
        }
        return cal.getTimeInMillis();
    }

    private long[] add(long[] cache, long ldate, long udate) {
//        SimpleDateFormat fmt = new SimpleDateFormat(DATE_FORMAT);
//        fmt.setTimeZone(tz);
//        String l = fmt.format(ldate);
//        String u = fmt.format(udate);
        long[] ncache = Arrays.copyOf(cache, cache.length + 2);
        ncache[cache.length] = ldate;
        ncache[cache.length + 1] = udate;
        return ncache;
    }

    private long toDate(int[] r) {
        try {
            Object[] args = new Object[7];
            for(int i = 0; i != 7; ++i) {
                args[i] = r[i];
            }
            SimpleDateFormat fmt = new SimpleDateFormat(DATE_FORMAT);
            fmt.setTimeZone(tz);
            String date = String.format("%02d%02d.%02d.%02d_%02d:%02d:%02d", args);
            return fmt.parse(date).getTime();
        } catch (ParseException e) {
            return -1;
        }
    }

    private boolean match(long[] cache, long timestamp) {
        int n = 0;
        while(n < cache.length) {
            if (cache[n] <= timestamp && cache[n + 1] > timestamp) {
                return true;
            }
            n += 2;
        }
        return false;
    }
}
