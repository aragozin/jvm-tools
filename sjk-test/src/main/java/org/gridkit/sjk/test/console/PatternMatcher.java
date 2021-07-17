package org.gridkit.sjk.test.console;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PatternMatcher {

    private final List<String> lines = new ArrayList<String>();

    private final PatternNode pattern;

    private final Map<LineMatcherNode, boolean[]> matchCache = new HashMap<LineMatcherNode, boolean[]>();

    public PatternMatcher(PatternNode pattern) {
        this.pattern = pattern;
    }

    public List<String> lines() {
        return lines;
    }

    public boolean matchWhole(String text) {
        matchCache.clear();
        lines.clear();

        String[] lines = text.split("\n");
        this.lines.addAll(Arrays.asList(lines));

        int[] matches = match(0, pattern);
        for (int m: matches) {
            if (m == this.lines.size()) {
                return true;
            }
        }
        return false;
    }

    public int matchStart(String text) {
        matchCache.clear();
        lines.clear();

        String[] lines = text.split("\n");
        this.lines.addAll(Arrays.asList(lines));

        int[] matches = match(0, pattern);

        return matches.length > 0 ? min(matches) : -1;
    }

    public String reportMatchProblems() {
        StringBuilder sb = new StringBuilder();
        for (LineMatcherNode node: matchCache.keySet()) {
            boolean[] cached = matchCache.get(node);
            boolean matched = false;
            for (int n = 0; n < cached.length; n += 2) {
                if (cached[n] && cached[n + 1]) {
                    matched = true;
                    break;
                }
            }
            if (!matched) {
                if (sb.length() > 0) {
                    sb.append("\n");
                }
                sb.append("Matcher is never matched: " + node);
            }
        }
        int lastMatched = -1;
        for (int n = 0; n < lines.size(); ++n) {
            if (hasAnyMatches(n)) {
                lastMatched = n;
            }
        }
        if (lastMatched + 1 < lines.size()) {
            if (sb.length() > 0) {
                sb.append("\n");
            }
            sb.append("Next unmatched line: " + lines.get(lastMatched + 1));
        }
        return sb.toString();
    }

    private boolean hasAnyMatches(int n) {
        for(boolean[] cached: matchCache.values()) {
            if (cached != null) {
                if (cached[2 * n] && cached[2 * n + 1]) {
                    return true;
                };
            }
        }
        return false;
    }


    private int min(int[] matches) {
        int min = matches[0];
        for (int i = 1; i < matches.length; ++i) {
            min = Math.min(min, matches[i]);
        }
        return min;
    }

    private int[] match(int offset, PatternNode pattern) {
        if (pattern instanceof LineMatcherNode) {
            return matchLineMatcher(offset, (LineMatcherNode) pattern);

        } else if (pattern instanceof AnyLinesNode) {
            AnyLinesNode any = (AnyLinesNode)pattern;
            int maxMatches = lines.size() - offset + 1;
            if (any.maxMatches > 0) {
                maxMatches = Math.min(maxMatches, any.maxMatches);
            }
            if (any.minMatches > 0) {
                maxMatches -= any.minMatches;
            }
            if (maxMatches < 1) {
                return new int[0];
            }
            int[] matches = new int[lines.size() - offset + 1];
            for (int n = 0; n != matches.length; ++n) {
                matches[n] = lines.size() - n + any.minMatches;
            }
            return matches;

        } else if (pattern instanceof SequenceNode) {
            return matchSequence(offset, (SequenceNode) pattern);

        } else if (pattern instanceof AlternativesNode) {
            return matchAlternatives(offset, (AlternativesNode) pattern);

        } else {

            throw new IllegalArgumentException("Unlnown node: " + pattern);
        }
    }

    private int[] matchLineMatcher(int offset, LineMatcherNode node) {
        if (matchLine(offset, node)) {
            return new int[] {offset + 1};
        } else {
            return new int[0];
        }
    }

    private int[] matchSequence(int offset, SequenceNode node) {
        return matchSequence(offset, node, 0);
    }

    private int[] matchSequence(int offset, SequenceNode node, int seqOffs) {

        if (node.nodes.length == seqOffs) {
            return new int[] {offset};
        } else {

            PatternNode element = node.nodes[seqOffs];
            int[] imatches = match(offset, element);

            int[] list = new int[16];
            int mc = 0;

            for (int m: imatches) {
                int[] f = matchSequence(m, node, seqOffs + 1);

                int ns = mc + f.length;
                if (ns > list.length) {
                    list = Arrays.copyOf(list, ns + 16);
                }
                System.arraycopy(f, 0, list, mc, f.length);
                mc = ns;
            }

            return Arrays.copyOf(list, mc);
        }
    }

    private int[] matchAlternatives(int offset, AlternativesNode node) {

        int[] list = new int[16];
        int mc = 0;

        for (PatternNode alt: node.nodes) {
            int[] match = match(offset, alt);
            if (match.length > 0) {
                int ns = mc + match.length;
                if (list.length < ns) {
                    list = Arrays.copyOf(list, ns + 16);
                }
                System.arraycopy(match, 0, list, mc, match.length);
                mc = ns;
            }
        }

        return Arrays.copyOf(list, mc);
    }

    private boolean matchLine(int idx, LineMatcherNode node) {
        if (idx >= lines.size()) {
            return false;
        }
        boolean[] cache = matchCache.get(node);
        if (cache == null) {
            matchCache.put(node, cache = new boolean[2 * lines.size()]);
        }
        if (cache[2 * idx]) {
            return cache[2 * idx + 1];
        }
        else {
            boolean match = node.match(lines.get(idx));
            cache[2 * idx] = true;
            cache[2 * idx + 1] = match;
            return match;
        }
    }

    public static interface LineMatcher {

        public boolean match(String line);

    }

    public static abstract class PatternNode {
    }

    public static abstract class LineMatcherNode extends PatternNode implements LineMatcher {
    }

    public static class MatcherNode extends LineMatcherNode {

        private final LineMatcher matcher;

        public MatcherNode(LineMatcher matcher) {
            this.matcher = matcher;
        }

        @Override
        public boolean match(String line) {
            return matcher.match(line);
        }

        @Override
        public String toString() {
            return matcher.toString();
        }
    }

    public static class AnyLinesNode extends PatternNode {

        private int minMatches;
        private int maxMatches;

        public AnyLinesNode() {
            minMatches = 0;
            maxMatches = -1;
        }

        public AnyLinesNode(int minMatches, int maxMatches) {
            this.minMatches = minMatches;
            this.maxMatches = maxMatches;
        }

        @Override
        public String toString() {
            return (minMatches == 0 && maxMatches == -1)
                    ? "ANY"
                    : "ANY[" + ((minMatches > 0) ? ("min=" + maxMatches) : maxMatches > 0 ? "," : "")
                        + ((maxMatches > 0) ? ("max=" + maxMatches) : "") + "]";
        }
    }

    public static class SequenceNode extends PatternNode {

        private final PatternNode[] nodes;

        public SequenceNode(PatternNode... nodes) {
            this.nodes = nodes;
        }

        public SequenceNode append(PatternNode node) {
            PatternNode[] nn = Arrays.copyOf(nodes, nodes.length + 1);
            nn[nn.length - 1] = node;
            return new SequenceNode(nn);
        }

        @Override
        public String toString() {
            return "SEQ" + Arrays.toString(nodes);
        }
    }

    public static class AlternativesNode extends PatternNode {

        private final PatternNode[] nodes;

        public AlternativesNode(PatternNode... nodes) {
            this.nodes = nodes;
        }

        public AlternativesNode append(PatternNode node) {
            PatternNode[] nn = Arrays.copyOf(nodes, nodes.length + 1);
            nn[nn.length - 1] = node;
            return new AlternativesNode(nn);
        }

        @Override
        public String toString() {
            return "ALT" + Arrays.toString(nodes);
        }
    }

    public static class MatchExact extends LineMatcherNode {

        private final String line;

        public MatchExact(String line) {
            this.line = line;
        }

        @Override
        public boolean match(String line) {
            return this.line.equals(line);
        }

        @Override
        public String toString() {
            return "Exact[" + line + "]";
        }
    }
}
