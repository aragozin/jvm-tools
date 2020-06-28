/**
 * Copyright 2013 Alexey Ragozin
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.gridkit.jvmtool;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Alexey Ragozin (alexey.ragozin@gmail.com)
 */
public class GlobHelper {

    /**
     * GLOB pattern supports *, ** and ? wild cards.
     * Leading and trailing ** have special meaning, consecutive separator become optional.
     */
    public static Pattern translate(String pattern, String separator) {
        StringBuffer sb = new StringBuffer();
        String es = escape(separator);
        // special starter
        Matcher ss = Pattern.compile("^([*][*][" + es + "]).*").matcher(pattern);
        if (ss.matches()) {
            pattern = pattern.substring(ss.group(1).length());
            // make leading sep optional
            sb.append("(.*[" + es + "])?");
        }
        // special trailer
        Matcher st = Pattern.compile(".*([" + es + "][*][*])$").matcher(pattern);
        boolean useSt = false;
        if (st.matches()) {
            pattern = pattern.substring(0, st.start(1));
            useSt = true;
        }

        for(int i = 0; i != pattern.length(); ++i) {
            char c = pattern.charAt(i);
            if (c == '?') {
                sb.append("[^" + es + "]");
            }
            else if (c == '*') {
                if (i + 1 < pattern.length() && pattern.charAt(i+1) == '*') {
                    i++;
                    // **
                    sb.append(".*");
                }
                else {
                    sb.append("[^" + es + "]*");
                }
            }
            else {
                if (c == '$') {
                    sb.append("\\$");
                }
                else if (Character.isJavaIdentifierPart(c) || Character.isWhitespace(c)) {
                    sb.append(c);
                }
                else {
                    sb.append('\\').append(c);
                }
            }
        }

        if (useSt) {
            sb.append("([" + es + "].*)?");
        }

        return Pattern.compile(sb.toString());
    }

    private static String escape(String separator) {
        StringBuffer sb = new StringBuffer();
        for(int i = 0; i != separator.length(); ++i) {
            char c = separator.charAt(i);
            if ("\\[]&-".indexOf(c) >= 0){
                sb.append('\\').append(c);
            }
            else {
                sb.append(c);
            }
        }
        return sb.toString();
    }
}
