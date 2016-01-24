package org.gridkit.jvmtool.stacktrace.analytics;

import java.lang.Thread.State;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.gridkit.jvmtool.stacktrace.CounterCollection;
import org.gridkit.jvmtool.stacktrace.StackFrame;
import org.gridkit.jvmtool.stacktrace.StackFrameList;
import org.gridkit.jvmtool.stacktrace.ThreadSnapshot;
import org.gridkit.jvmtool.stacktrace.analytics.ClassificatorAST.AndCombinatorFilter;
import org.gridkit.jvmtool.stacktrace.analytics.ClassificatorAST.AnyOfFrameMatcher;
import org.gridkit.jvmtool.stacktrace.analytics.ClassificatorAST.LastFollowedFilter;
import org.gridkit.jvmtool.stacktrace.analytics.ClassificatorAST.LastNotFollowedFilter;
import org.gridkit.jvmtool.stacktrace.analytics.ClassificatorAST.OrCombinatorFilter;
import org.gridkit.jvmtool.stacktrace.analytics.ClassificatorAST.PatternFilter;

/**
 * Default implementation of factory is producing thread safe filter
 * implementations.
 * <br/>
 * See {@link CachingFilterFactory} for optimized single threaded version.
 *  
 * @author Alexey Ragozin (alexey.ragozin@gmail.com)
 */
public class BasicFilterFactory {

    public ThreadSnapshotFilter build(ClassificatorAST.Filter filter) {
        if (filter instanceof AndCombinatorFilter) {
            List<ThreadSnapshotFilter> list = new ArrayList<ThreadSnapshotFilter>();
            for(ClassificatorAST.Filter f: ((AndCombinatorFilter)filter).subfilters) {
                if (!(f instanceof ClassificatorAST.TrueFilter)) {
                    if (f instanceof ClassificatorAST.FalseFilter) {
                        return falseFilter();
                    }
                    list.add(build(f));
                }
            }
            if (list.isEmpty()) {
                return trueFilter();
            }
            else {
                return disjunction(list);
            }
        }
        else if (filter instanceof OrCombinatorFilter) {
            List<ThreadSnapshotFilter> list = new ArrayList<ThreadSnapshotFilter>();
            for(ClassificatorAST.Filter f: ((OrCombinatorFilter)filter).subfilters) {
                if (!(f instanceof ClassificatorAST.FalseFilter)) {
                    if (f instanceof ClassificatorAST.TrueFilter) {
                        return trueFilter();
                    }
                    list.add(build(f));
                }
            }
            if (list.isEmpty()) {
                return falseFilter();
            }
            else {
                return conjunction(list);
            }
        }
        else if (filter instanceof ClassificatorAST.TrueFilter) {
            return trueFilter();
        }
        else if (filter instanceof ClassificatorAST.FalseFilter) {
            return falseFilter();
        }
        else if (filter instanceof ClassificatorAST.LastFollowedFilter) {
            LastFollowedFilter lff = (LastFollowedFilter) filter;
            return followed(lastFrame(build(lff.snippet)), build(lff.followFilter));
        }
        else if (filter instanceof ClassificatorAST.LastNotFollowedFilter) {
            LastNotFollowedFilter lff = (LastNotFollowedFilter) filter;
            return followed(lastFrame(build(lff.snippet)), not(build(lff.followFilter)));
        }
        else if (filter instanceof ClassificatorAST.PatternFilter) {
            PatternFilter pf = (PatternFilter) filter;
            return frameFilter(patternFrameMatcher(pf.patterns));
        }
        else {
            throw new IllegalArgumentException("Unknow AST node: " + filter);
        }
    }

    public StackFrameMatcher build(ClassificatorAST.FrameMatcher matcher) {
        if (matcher instanceof ClassificatorAST.PatternFilter) {
            PatternFilter pf = (PatternFilter) matcher;
            return patternFrameMatcher(pf.patterns);
        }
        else if (matcher instanceof ClassificatorAST.FalseFilter) {
            return falseFrameMatcher();
        }
        else if (matcher instanceof ClassificatorAST.AnyOfFrameMatcher) {
            AnyOfFrameMatcher any = (AnyOfFrameMatcher) matcher;
            List<String> patterns = new ArrayList<String>();
            List<StackFrameMatcher> list = new ArrayList<StackFrameMatcher>();
            for(ClassificatorAST.FrameMatcher m: any.submatchers) {
                if (m instanceof ClassificatorAST.FalseFilter) {
                    continue;
                }
                else if (m instanceof ClassificatorAST.PatternFilter) {
                    patterns.addAll(((PatternFilter)m).patterns);
                }
                else {
                    list.add(build(m));
                }
            }
            if (!patterns.isEmpty()) {
                list.add(patternFrameMatcher(patterns));
            }
            if (list.isEmpty()) {
                return falseFrameMatcher();
            }
            else if (list.size() == 1) {
                return list.get(0);
            }
            else {
                return matcherConjunction(list);
            }            
        }
        else {
            throw new IllegalArgumentException("Unknown ASt node: " + matcher);
        }
    }

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
