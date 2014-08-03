package org.gridkit.jvmtool.heapdump;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.netbeans.lib.profiler.heap.Instance;
import org.netbeans.lib.profiler.heap.JavaClass;

class TypeFilterStep extends PathStep {

    private final String pattern;
    private final List<MatchOption> matchers = new ArrayList<TypeFilterStep.MatchOption>();

    TypeFilterStep(String pattern) {
        this.pattern = pattern;
        initMatchers(pattern);
    }

    public boolean evaluate(JavaClass jc) {
        return match(jc);
    }

    public Collection<JavaClass> filter(Collection<JavaClass> col) {
        List<JavaClass> result = new ArrayList<JavaClass>();
        for(JavaClass jc: col) {
            if (match(jc)) {
                result.add(jc);
            }
        }
        return result;
    }

    @Override
    public Iterator<Instance> walk(Instance instance) {
        if (instance != null && match(instance.getJavaClass())) {
            return Collections.singleton(instance).iterator();
        }
        else {
            return Collections.<Instance>emptyList().iterator();
        }
    }

    @Override
    public Iterator<Move> track(Instance instance) {
        if (instance != null && match(instance.getJavaClass())) {
            return Collections.singleton(new Move("", instance)).iterator();
        }
        else {
            return Collections.<Move>emptyList().iterator();
        }
    }

    private boolean match(JavaClass javaClass) {
        String name = javaClass.getName();
        boolean checkSuper = false;
        for(MatchOption m: matchers) {
            if (m.pattern.matcher(name).matches()) {
                return true;
            }
            checkSuper |= m.hierarchy;
        }

        if (checkSuper) {
            JavaClass cc = javaClass.getSuperClass();
            while(cc != null) {
                name = cc.getName();
                for(MatchOption m: matchers) {
                    if (m.hierarchy) {
                        if (m.pattern.matcher(name).matches()) {
            return true;
        }
            }
                }
                cc = cc.getSuperClass();
            }
        }

            return false;
        }

    private void initMatchers(String pattern) {
        String[] parts = pattern.split("[|]");
        for(String part: parts) {
            MatchOption opt = new MatchOption();
            if (part.startsWith("+")) {
                opt.hierarchy = true;
                part = part.substring(1);
            }
            opt.pattern = translate(part, ".");
            matchers.add(opt);
        }
    }

    /**
     * GLOB pattern supports *, ** and ? wild cards.
     * Leading and trailing ** have special meaning, consecutive separator become optional.
     */
    private static Pattern translate(String pattern, String separator) {
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
                else if (Character.isJavaIdentifierPart(c) || Character.isWhitespace(c) || c == '|') {
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

    @Override
    public String toString() {
        return "(" + pattern + ")";
    }

    private static class MatchOption {

        boolean hierarchy;
        Pattern pattern;

    }
}
