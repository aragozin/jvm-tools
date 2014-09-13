package org.gridkit.jvmtool;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public interface StackTraceFilter {

    public boolean evaluate(StackTraceElement[] trace);

    public class AnyMatcher implements StackTraceFilter {

        StackTraceFilter[] alternatives;

        public AnyMatcher(StackTraceFilter[] alternatives) {
            this.alternatives = alternatives;
        }

        @Override
        public boolean evaluate(StackTraceElement[] trace) {
            for(StackTraceFilter alternative: alternatives) {
                if (alternative.evaluate(trace)) {
                    return true;
                }
            }
            return false;
        }
    }

    public class RequiredMatcher implements StackTraceFilter {

        StackTraceFilter[] requires;

        public RequiredMatcher(StackTraceFilter[] requires) {
            this.requires = requires;
        }

        @Override
        public boolean evaluate(StackTraceElement[] trace) {
            for(StackTraceFilter r: requires) {
                if (!r.evaluate(trace)) {
                    return false;
                }
            }
            return true;
        }
    }

    public class HasElementMatcher implements StackTraceFilter {

        CachingElementMatcher matcher;

        public HasElementMatcher(CachingElementMatcher matcher) {
            super();
            this.matcher = matcher;
        }

        @Override
        public boolean evaluate(StackTraceElement[] trace) {
            for(StackTraceElement e: trace) {
                if (matcher.evaluate(e)) {
                    return true;
                }
            }
            return false;
        }
    }

    public class PositionalMatcher implements StackTraceFilter {

        CachingElementMatcher targetElementMatcher;
        StackTraceFilter predicate;
        boolean topDown;
        boolean firstOnly;
        boolean lastOnly;
        boolean singleElementPredicate;

        @Override
        public boolean evaluate(StackTraceElement[] trace) {
            if (topDown) {
                int first = -1;
                int last = -1;

                for(int i = trace.length; i != 0; --i) {
                    StackTraceElement e = trace[i - 1];
                    if (targetElementMatcher.evaluate(e)) {
                        if (first == -1) {
                            first = i - 1;
                        }
                        last = i - 1;
                        if (!firstOnly && !lastOnly) {
                            StackTraceElement[] part = cut(trace, i - 1);
                            return predicate.evaluate(part);
                        }
                        if (firstOnly) {
                            break;
                        }
                    }
                }

                if (!firstOnly && !lastOnly) {
                    return false;
                }

                if (firstOnly && lastOnly && (first != last)) {
                    return false;
                }

                int n = firstOnly ? first : last;

                if (n < 0) {
                    return false;
                }

                StackTraceElement[] part = cut(trace, n);
                return predicate.evaluate(part);
            }
            else {
                throw new UnsupportedOperationException();
            }
        }

        private StackTraceElement[] cut(StackTraceElement[] trace, int i) {
            if (topDown) {
                if (singleElementPredicate) {
                    return i == 0 ? new StackTraceElement[0] : new StackTraceElement[]{trace[i - 1]};
                }
                else {
                    StackTraceElement[] part = new StackTraceElement[i];
                    System.arraycopy(trace, 0, part, 0, part.length);
                    return part;
                }
            }
            else {
                if (singleElementPredicate) {
                    return i == trace.length - 1 ? new StackTraceElement[0] : new StackTraceElement[]{trace[i + 1]};
                }
                else {
                    StackTraceElement[] part = new StackTraceElement[trace.length - i - 1];
                    System.arraycopy(trace, i + 1, part, 0, part.length);
                    return part;
                }
            }
        }
    }

    public class PatternElementMatcher extends CachingElementMatcher {

        private Pattern pattern;

        public PatternElementMatcher(String... patterns) {
            StringBuilder sb = new StringBuilder();
            sb.append("(");
            for(String p: patterns) {
                String pat = GlobHelper.translate(p, ".").pattern();
                sb.append(pat).append(")|(");
            }
            sb.setLength(sb.length() - 2);

            pattern = Pattern.compile(sb.toString());
        }

        @Override
        protected boolean test(StackTraceElement ste) {
            Matcher m = pattern.matcher(ste.toString());
            return m.lookingAt();
        }
    }

//    public class MatcherFilter implements StackTraceFilter {
//
//        private CachingElementMatcher matcher;
//
//        public MatcherFilter()
//
//        @Override
//        public boolean evaluate(StackTraceElement[] trace) {
//            // TODO Auto-generated method stub
//            return false;
//        }
//
//    }

    public abstract class CachingElementMatcher {

        protected Set<StackTraceElement> matched = new HashSet<StackTraceElement>();
        protected Set<StackTraceElement> unmatched = new HashSet<StackTraceElement>();

        public boolean evaluate(StackTraceElement ste) {
            if (matched.contains(ste)) {
                return true;
            }
            else if (unmatched.contains(ste)) {
                return false;
            }
            else {
                boolean match = test(ste);
                if (match) {
                    matched.add(ste);
                }
                else {
                    unmatched.add(ste);
                }
                return match;
            }
        }

        protected abstract boolean test(StackTraceElement ste);

    }

}
