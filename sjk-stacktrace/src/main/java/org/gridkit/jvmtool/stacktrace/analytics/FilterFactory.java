package org.gridkit.jvmtool.stacktrace.analytics;

import java.util.Collection;

import org.gridkit.jvmtool.stacktrace.analytics.ClassificatorAST.Filter;
import org.gridkit.jvmtool.stacktrace.analytics.ClassificatorAST.FrameMatcher;


public class FilterFactory {

    private static final BasicFilterFactory DEFAULT_FACTORY = new BasicFilterFactory();

    public static ThreadSnapshotFilter build(Filter filter) {
        return DEFAULT_FACTORY.build(filter);
    }

    public static StackFrameMatcher build(FrameMatcher matcher) {
        return DEFAULT_FACTORY.build(matcher);
    }

    public static ThreadSnapshotFilter disjunction(ThreadSnapshotFilter... subfilters) {
        return DEFAULT_FACTORY.disjunction(subfilters);
    }

    public static ThreadSnapshotFilter disjunction(Collection<ThreadSnapshotFilter> subfilters) {
        return DEFAULT_FACTORY.disjunction(subfilters);
    }

    public static ThreadSnapshotFilter conjunction(ThreadSnapshotFilter... subfilters) {
        return DEFAULT_FACTORY.conjunction(subfilters);
    }

    public static ThreadSnapshotFilter conjunction(Collection<ThreadSnapshotFilter> subfilters) {
        return DEFAULT_FACTORY.conjunction(subfilters);
    }

    public static StackFrameMatcher matcherConjunction(StackFrameMatcher... subfilters) {
        return DEFAULT_FACTORY.matcherConjunction(subfilters);
    }

    public static StackFrameMatcher matcherConjunction(Collection<StackFrameMatcher> subfilters) {
        return DEFAULT_FACTORY.matcherConjunction(subfilters);
    }

    public static ThreadSnapshotFilter not(ThreadSnapshotFilter filter) {
        return DEFAULT_FACTORY.not(filter);
    }

    public static ThreadSnapshotFilter followed(PositionalStackMatcher matcher, ThreadSnapshotFilter filter) {
        return DEFAULT_FACTORY.followed(matcher, filter);
    }

    public static ThreadSnapshotFilter frameFilter(StackFrameMatcher matcher) {
        return DEFAULT_FACTORY.frameFilter(matcher);
    }

    public static ThreadSnapshotFilter falseFilter() {
        return DEFAULT_FACTORY.falseFilter();
    }

    public static StackFrameMatcher falseFrameMatcher() {
        return DEFAULT_FACTORY.falseFrameMatcher();
    }

    public static ThreadSnapshotFilter trueFilter() {
        return DEFAULT_FACTORY.trueFilter();
    }

    public static StackFrameMatcher patternFrameMatcher(String... patterns) {
        return DEFAULT_FACTORY.patternFrameMatcher(patterns);
    }

    public static StackFrameMatcher patternFrameMatcher(Collection<String> patterns) {
        return DEFAULT_FACTORY.patternFrameMatcher(patterns);
    }

    public static PositionalStackMatcher lastFrame(StackFrameMatcher matcher) {
        return DEFAULT_FACTORY.lastFrame(matcher);
    }
}
