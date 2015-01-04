package org.gridkit.jvmtool;

import java.util.Collection;

import org.gridkit.jvmtool.StackTraceFilter.ElementMatcher;
import org.gridkit.jvmtool.StackTraceFilter.PatternElementMatcher;
import org.gridkit.jvmtool.StackTraceFilter.PositionalMatcher;

public class StackTraceFilterHelper {

    private static StackTraceFilter TRUE_FILTER = new StackTraceFilter() {

        @Override
        public boolean evaluate(StackTraceElement[] trace) {
            return true;
        }
    };

    public static StackTraceFilter trueFilter() {
        return TRUE_FILTER;
    }

    public static ElementMatcher createElementMatcher(String... patterns) {
        PatternElementMatcher matcher = new PatternElementMatcher(patterns);
        return matcher;
    }

    public static ElementMatcher createElementMatcher(Collection<String> patterns) {
        PatternElementMatcher matcher = new PatternElementMatcher(patterns.toArray(new String[patterns.size()]));
        return matcher;
    }

    public static StackTraceFilter createElementMatcherFilter(ElementMatcher matcher) {
        return new StackTraceFilter.HasElementMatcher(matcher);
    }

    public static StackTraceFilter createFilterDisjunction(Collection<StackTraceFilter> filters) {
        return new StackTraceFilter.RequiredMatcher(filters.toArray(new StackTraceFilter[filters.size()]));
    }

    public static StackTraceFilter createFilterConjunction(Collection<StackTraceFilter> filters) {
        return new StackTraceFilter.AnyMatcher(filters.toArray(new StackTraceFilter[filters.size()]));
    }

    public static StackTraceFilter createLastFollowedMatcher(ElementMatcher elementMatcher, StackTraceFilter predicate) {
        PositionalMatcher matcher = new PositionalMatcher();
        matcher.targetElementMatcher = elementMatcher;
        matcher.predicate = predicate;
        matcher.topDown = true;
        matcher.lastOnly = true;
        matcher.singleElementPredicate = true;
        return matcher;
    }

    public static StackTraceFilter createLastNotFollowedMatcher(ElementMatcher matcher, final StackTraceFilter predicate) {
        final StackTraceFilter inverted = new StackTraceFilter() {
            @Override
            public boolean evaluate(StackTraceElement[] trace) {
                return !predicate.evaluate(trace);
            }

        };
        return createLastFollowedMatcher(matcher, inverted);
    }
}
