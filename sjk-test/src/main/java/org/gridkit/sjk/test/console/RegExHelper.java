package org.gridkit.sjk.test.console;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

class RegExHelper {

    private static final Pattern TOKENIZER = Pattern.compile(
            "\\\\[(]"
            + "|\\[.*\\]"
            + "|\\\\Q.*\\\\E"
            + "|.");

    public static String uncapture(String pattern) {
        StringBuilder sb = new StringBuilder();
        Matcher m = TOKENIZER.matcher(pattern);
        int n = 0;
        while(m.find(n)) {
            String tkn = m.group();
            if ("(".equals(tkn)) {
                sb.append("(?:");
            } else {
                sb.append(tkn);
            }
            n = m.end();
        }
        return sb.toString();
    }
}
