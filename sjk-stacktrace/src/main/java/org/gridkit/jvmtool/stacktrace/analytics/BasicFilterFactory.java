package org.gridkit.jvmtool.stacktrace.analytics;

import java.lang.Thread.State;
import java.util.Arrays;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.gridkit.jvmtool.event.TagCollection;
import org.gridkit.jvmtool.stacktrace.CounterCollection;
import org.gridkit.jvmtool.stacktrace.StackFrame;
import org.gridkit.jvmtool.stacktrace.StackFrameList;
import org.gridkit.jvmtool.stacktrace.ThreadSnapshot;

/**
 * Default implementation of factory is producing thread safe filter
 * implementations.
 * <br/>
 * See {@link CachingFilterFactory} for optimized single threaded version.
 *
 * @author Alexey Ragozin (alexey.ragozin@gmail.com)
 */
public class BasicFilterFactory {

    public ThreadSnapshotFilter disjunction(ThreadSnapshotFilter... subfilters) {
        return disjunction(Arrays.asList(subfilters));
    }

    public ThreadSnapshotFilter disjunction(Collection<ThreadSnapshotFilter> subfilters) {
        if (subfilters.isEmpty()) {
            return trueFilter();
        }
        return new DisjunctionFilter(subfilters.toArray(new ThreadSnapshotFilter[0]));
    }

    public ThreadSnapshotFilter conjunction(ThreadSnapshotFilter... subfilters) {
        return conjunction(Arrays.asList(subfilters));
    }

    public ThreadSnapshotFilter conjunction(Collection<ThreadSnapshotFilter> subfilters) {
        if (subfilters.isEmpty()) {
            return falseFilter();
        }
        return new ConjunctionFilter(subfilters.toArray(new ThreadSnapshotFilter[0]));
    }

    public StackFrameMatcher matcherConjunction(StackFrameMatcher... subfilters) {
        return matcherConjunction(Arrays.asList(subfilters));
    }

    public StackFrameMatcher matcherConjunction(Collection<StackFrameMatcher> subfilters) {
        if (subfilters.isEmpty()) {
            return falseFrameMatcher();
        }
        return new ConjunctionMatcher(subfilters.toArray(new StackFrameMatcher[0]));
    }

    public ThreadSnapshotFilter not(final ThreadSnapshotFilter filter) {
        return new ThreadSnapshotFilter() {
            @Override
            public boolean evaluate(ThreadSnapshot snapshot) {
                return !filter.evaluate(snapshot);
            }
        };
    }

    public ThreadSnapshotFilter followed(PositionalStackMatcher matcher, ThreadSnapshotFilter filter) {
        return new FollowedPredicate(matcher, filter);
    }

    public ThreadSnapshotFilter frameFilter(final StackFrameMatcher matcher) {
        return new ThreadSnapshotFilter() {
            @Override
            public boolean evaluate(ThreadSnapshot snapshot) {
                for(StackFrame frame: snapshot.stackTrace()) {
                    if (matcher.evaluate(frame)) {
                        return true;
                    }
                }
                return false;
            }
        };
    }

    public ThreadSnapshotFilter falseFilter() {
        return new FalseFilter();
    }

    public StackFrameMatcher falseFrameMatcher() {
        return new FalseMatcher();
    }

    public ThreadSnapshotFilter trueFilter() {
        return new TrueFilter();
    }

    public ThreadSnapshotFilter threadStateMatter(String matcher) {
        return new ThreadStateMatcher(matcher);
    }

    public StackFrameMatcher patternFrameMatcher(String... patterns) {
        return patternFrameMatcher(Arrays.asList(patterns));
    }

    public StackFrameMatcher patternFrameMatcher(Collection<String> patterns) {
        if (patterns.isEmpty()) {
            throw new IllegalArgumentException("Pattern list is empty");
        }
        return new PatternFrameMatcher(patterns);
    }

    public PositionalStackMatcher lastFrame(StackFrameMatcher matcher) {
        return new LastFrameMatcher(matcher);
    }

    public PositionalStackMatcher firstFrame(StackFrameMatcher matcher) {
        return new FirstFrameMatcher(matcher);
    }

    protected final class TrueFilter implements ThreadSnapshotFilter {
        @Override
        public boolean evaluate(ThreadSnapshot snapshot) {
            return true;
        }
    }

    protected final class FalseMatcher implements StackFrameMatcher {
        @Override
        public boolean evaluate(StackFrame frame) {
            return false;
        }
    }

    protected final class FalseFilter implements ThreadSnapshotFilter {
        @Override
        public boolean evaluate(ThreadSnapshot snapshot) {
            return false;
        }
    }

    protected class LastFrameMatcher implements PositionalStackMatcher {

        private final StackFrameMatcher matcher;

        public LastFrameMatcher(StackFrameMatcher matcher) {
            this.matcher = matcher;
        }

        @Override
        public int matchNext(ThreadSnapshot snap, int matchFrom) {
            StackFrameList trace = snap.stackTrace();
            if (matchFrom > 0) {
                // assume that match have been found already
                return -1;
            }
            for(int i = matchFrom; i < trace.depth(); ++i) {
                if (matcher.evaluate(trace.frameAt(i))) {
                    return i;
                }
            }
            return -1;
        }
    }

    protected class FirstFrameMatcher implements PositionalStackMatcher {

        private final StackFrameMatcher matcher;

        public FirstFrameMatcher(StackFrameMatcher matcher) {
            this.matcher = matcher;
        }

        @Override
        public int matchNext(ThreadSnapshot snap, int matchFrom) {
            StackFrameList trace = snap.stackTrace();
            if (matchFrom > 0) {
                // assume that match have been found already
                return -1;
            }
            for(int i = trace.depth(); i > 0; --i) {
                if (matcher.evaluate(trace.frameAt(i - 1))) {
                    return i;
                }
            }
            return -1;
        }
    }

    protected static class FollowedPredicate implements ThreadSnapshotFilter, PositionalStackMatcher {

        private final PositionalStackMatcher matcher;
        private final ThreadSnapshotFilter tailFilter;

        public FollowedPredicate(PositionalStackMatcher matcher, ThreadSnapshotFilter tailFilter) {
            this.matcher = matcher;
            this.tailFilter = tailFilter;
        }

        @Override
        public boolean evaluate(ThreadSnapshot snapshot) {
            int n = -1;
            while(true) {
                int m = matcher.matchNext(snapshot, n + 1);
                if (m < 0) {
                    break;
                }
                n = m;
            }
            if (n >= 0) {
                StackFrameList remained = snapshot.stackTrace();
                remained = remained.fragment(0, n);
                return tailFilter.evaluate(new ThreadSnapProxy(snapshot, remained));
            }
            else {
                return false;
            }
        }

        @Override
        public int matchNext(ThreadSnapshot snap, int matchFrom) {
            int n = matchFrom - 1;
            while(true) {
                int m = matcher.matchNext(snap, n + 1);
                if (m < 0) {
                    if (n >= matchFrom) {
                        StackFrameList remained = snap.stackTrace();
                        remained = remained.fragment(0, n);
                        if (tailFilter.evaluate(new ThreadSnapProxy(snap, remained))) {
                            return n;
                        }
                    }
                    return -1;
                }
                n = m;
            }
        }
    }

    protected static class DisjunctionFilter implements ThreadSnapshotFilter {

        private final ThreadSnapshotFilter[] filters;

        public DisjunctionFilter(ThreadSnapshotFilter[] filters) {
            this.filters = filters;
        }

        @Override
        public boolean evaluate(ThreadSnapshot snapshot) {
            for(ThreadSnapshotFilter f: filters) {
                if (!f.evaluate(snapshot)) {
                    return false;
                }
            }
            return true;
        }
    }

    protected static class ConjunctionFilter implements ThreadSnapshotFilter {

        private final ThreadSnapshotFilter[] filters;

        public ConjunctionFilter(ThreadSnapshotFilter[] filters) {
            this.filters = filters;
        }

        @Override
        public boolean evaluate(ThreadSnapshot snapshot) {
            for(ThreadSnapshotFilter f: filters) {
                if (f.evaluate(snapshot)) {
                    return true;
                }
            }
            return false;
        }
    }

    protected static class ConjunctionMatcher implements StackFrameMatcher {

        private final StackFrameMatcher[] matchers;

        public ConjunctionMatcher(StackFrameMatcher[] matcher) {
            this.matchers = matcher;
        }

        @Override
        public boolean evaluate(StackFrame frame) {
            for(StackFrameMatcher f: matchers) {
                if (f.evaluate(frame)) {
                    return true;
                }
            }
            return false;
        }
    }

    protected static class PatternFrameMatcher implements StackFrameMatcher {

        private final Pattern regEx;

        PatternFrameMatcher(Collection<String> patterns) {
            StringBuilder sb = new StringBuilder();
            sb.append('(');
            for(String pattern: patterns) {
                sb.append(wildCardTranslate(pattern));
                sb.append('|');
            }
            sb.setCharAt(sb.length() - 1, ')');
            regEx = Pattern.compile(sb.toString());
        }

        @Override
        public boolean evaluate(StackFrame frame) {
            return regEx.matcher(frame).lookingAt();
        }
    }

    protected static class ThreadStateMatcher implements ThreadSnapshotFilter {

        private final EnumSet<State> states;
        private final boolean matchNull;

        ThreadStateMatcher(String pattern) {
            Pattern regEx = Pattern.compile(wildCardTranslate(pattern));
            Set<State> st = new HashSet<State>();
            for(State s: State.values()) {
                if (regEx.matcher(s.toString()).matches()) {
                    st.add(s);
                }
            }
            states = st.isEmpty() ? EnumSet.noneOf(State.class) : EnumSet.copyOf(st);
            matchNull = regEx.matcher(String.valueOf((Object)null)).matches();
        }

        @Override
        public boolean evaluate(ThreadSnapshot snapshot) {
            State state = snapshot.threadState();
            if (state == null) {
                return matchNull;
            }
            else {
                return states.contains(state);
            }
        }
    }

    protected static class ThreadSnapProxy implements ThreadSnapshot {

        ThreadSnapshot snap;
        StackFrameList stack;

        public ThreadSnapProxy(ThreadSnapshot snap, StackFrameList stack) {
            this.snap = snap;
            this.stack = stack;
        }

        public long threadId() {
            return snap.threadId();
        }

        public String threadName() {
            return snap.threadName();
        }

        public long timestamp() {
            return snap.timestamp();
        }

        public StackFrameList stackTrace() {
            return stack != null ? stack : snap.stackTrace();
        }

        public State threadState() {
            return snap.threadState();
        }

        @Override
        public CounterCollection counters() {
            return snap.counters();
        }

        @Override
        public TagCollection tags() {
            return snap.tags();
        }
    }

    /**
     * GLOB pattern supports *, ** and ? wild cards.
     * Leading and trailing ** have special meaning, consecutive separator become optional.
     */
    protected static String wildCardTranslate(String pattern) {
        String separator = ".";
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

        return sb.toString();
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
