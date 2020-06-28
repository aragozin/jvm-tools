package org.gridkit.jvmtool.gcflow;

public class FormatHelper {

    public static String toMemoryUnits(double value) {
        if (value == 0) {
            return "0";
        }
        if (value < (10l << 10)) {
            String val = String.format("%.2f", value).trim();
            if (val.endsWith(".00")) {
                val = val.substring(0, val.length() - 3);
            }
            else if (val.indexOf('.') > 0 && val.endsWith("0")) {
                val = val.substring(0, val.length() - 1);
            }
            return val;
        }
        else if (value < (1l << 20)) {
            return String.format("%5.4gKi", value / (1l << 10)).trim();
        }
        else if (value < (1l << 30)) {
            return String.format("%5.4gMi", value / (1l << 20)).trim();
        }
        else if (value < (1l << 40)) {
            return String.format("%5.4gGi", value / (1l << 30)).trim();
        }
        else {
            return String.format("%dGi", (long)(value / (1l << 30))).trim();
        }
    }

    public static String toSeconds(double value) {
        return String.format("%6.3fB", value);
    }

}
