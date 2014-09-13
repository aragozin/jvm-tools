package org.gridkit.jvmtool;

import java.util.ArrayList;
import java.util.List;

import org.gridkit.jvmtool.StackTraceFilter.PatternElementMatcher;
import org.gridkit.jvmtool.StackTraceFilter.PositionalMatcher;

public abstract class StackFilterParser {

    public static final String D_REQUIRE = "REQUIRE";
    public static final String D_ANY = "ANY";
    public static final String D_LAST = "LAST";
    public static final String D_FOLLOWED = "FOLLOWED";
    public static final String D_NOT_FOLLOWED = "NOT FOLLOWED";

    public static StackFilterParser anyNode() {
        return new AnyNode();
    }

    public static StackFilterParser allNode() {
        return new AllNode();
    }

    public abstract StackTraceFilter getFilter();

    public static class AnyNode extends StackFilterParser {

        List<StackFilterParser> subnodes = new ArrayList<StackFilterParser>();

        @Cascade.Section
        public StackFilterParser child(String line) {
            if (line.startsWith("!")) {
                String dir = normalizeDirective(line);
                if (dir.equals(D_REQUIRE)) {
                    AllNode node = new AllNode();
                    subnodes.add(node);
                    return node;
                }
                else if (dir.equals(D_ANY)) {
                    AnyNode node = new AnyNode();
                    subnodes.add(node);
                    return node;
                }
                else if (dir.equals(D_LAST)) {
                    LastNode node = new LastNode();
                    subnodes.add(node);
                    return node;
                }
                else {
                    throw new IllegalArgumentException("Enexpected directive: " + dir);
                }
            }
            else {
                LiteralNode node = new LiteralNode(line);
                subnodes.add(node);
                return node;
            }
        }

        @Override
        public StackTraceFilter getFilter() {
            StackTraceFilter[] filters = new StackTraceFilter[subnodes.size()];
            int n = 0;
            for(StackFilterParser s: subnodes) {
                filters[n++] = s.getFilter();
            }
            return new StackTraceFilter.AnyMatcher(filters);
        }
    }

    public static class AllNode extends AnyNode {

        @Override
        public StackTraceFilter getFilter() {
            StackTraceFilter[] filters = new StackTraceFilter[subnodes.size()];
            int n = 0;
            for(StackFilterParser s: subnodes) {
                filters[n++] = s.getFilter();
            }
            return new StackTraceFilter.RequiredMatcher(filters);
        }
    }

    public static class LiteralNode extends StackFilterParser {

        String line;

        public LiteralNode(String line) {
            this.line = line;
        }

        @Cascade.Section
        public StackFilterParser child(String line) {
            if (line.startsWith("!")) {
                String dir = normalizeDirective(line);
                throw new IllegalArgumentException("Enexpected directive: " + dir);
            }
            else {
                throw new IllegalArgumentException("Enexpected nested line: " + line);
            }
        }

        @Override
        public StackTraceFilter getFilter() {
            final PatternElementMatcher matcher = new PatternElementMatcher(line);
            return new StackTraceFilter.HasElementMatcher(matcher);
        }
    }

    public static class LastNode extends StackFilterParser {

        List<String> targets = new ArrayList<String>();
        StackFilterParser predicate;
        boolean notMode;
        boolean directFollow;

        @Cascade.Section
        public StackFilterParser child(String line) {
            if (line.startsWith("!")) {
                String dir = normalizeDirective(line);

                if (predicate != null) {
                    throw new IllegalArgumentException("Could be only one directive");
                }
                if (targets.isEmpty()) {
                    throw new IllegalArgumentException("Matcher is required");
                }

                if (dir.equals(D_FOLLOWED)) {
                    directFollow = true;
                    return predicate = new AnyNode();
                }
                else if (dir.equals(D_NOT_FOLLOWED)) {
                    directFollow = true;
                    notMode = true;
                    return predicate = new AnyNode();
                }
                else {
                    throw new IllegalArgumentException("Enexpected directive: " + dir);
                }
            }
            else {
                if (predicate != null) {
                    throw new IllegalArgumentException("Should be no nested line after predicate directive");
                }
                targets.add(line);
                return null;
            }
        }

        @Override
        public StackTraceFilter getFilter() {
            if (predicate == null) {
                throw new IllegalArgumentException("!LAST statement is missing predicate");
            }
            if (targets.isEmpty()) {
                throw new IllegalArgumentException("!LAST statement is missing filter");
            }

            String[] m = targets.toArray(new String[targets.size()]);

            StackTraceFilter p = predicate.getFilter();
            if (notMode) {
                final StackTraceFilter nested = p;
                p = new StackTraceFilter() {
                    @Override
                    public boolean evaluate(StackTraceElement[] trace) {
                        return !nested.evaluate(trace);
                    }

                };
            }

            PatternElementMatcher matcher = new PatternElementMatcher(m);

            PositionalMatcher pm = new PositionalMatcher();
            pm.topDown = true;
            pm.lastOnly = true;
            pm.singleElementPredicate = directFollow;
            pm.targetElementMatcher = matcher;
            pm.predicate = p;

            return pm;
        }
    }

    static String normalizeDirective(String line) {
        return line.substring(1).replace("\\s+", " ").toUpperCase().trim();
    }
}
