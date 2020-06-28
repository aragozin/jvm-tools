package org.gridkit.jvmtool.stacktrace.analytics;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TraceFilterPredicateParser {

    private static final String REG_PAR = "[()]";
    private static final String REG_PATTERN = "[\\w\\d.:*$]+";
    private static final String REG_STATE_PATTERN = "#[Ss][Tt][Aa][Tt][Ee]=[\\w*]+";
    private static final String REG_COMMA = "[,]";
    private static final String REG_PLUS = "[+]";
    private static final String REG_EXCL = "[!]";
    private static final String REG_SLASH_PLUS = "[/][+]";
    private static final String REG_SLASH_EXCL = "[/][!]";
    private static final String REG_SLASH_UP_PLUS = "[/]\\^[+]";
    private static final String REG_SLASH_UP_EXCL = "[/]\\^[!]";

    static final Pattern TOKENIZER;
    static {
        String pattern = "("
                + "(" + REG_PAR + ")|"            	// 2
                + "(" + REG_PATTERN + ")|"        	// 3
                + "(" + REG_COMMA + ")|"          	// 4
                + "(" + REG_PLUS + ")|"          	// 5
                + "(" + REG_EXCL + ")|"           	// 6
                + "(" + REG_SLASH_PLUS + ")|"    	// 7
                + "(" + REG_SLASH_EXCL + ")|"   	// 8
                + "(" + REG_SLASH_UP_PLUS + ")|"	// 9
                + "(" + REG_SLASH_UP_EXCL + ")|"	// 10
                + "(" + REG_STATE_PATTERN + ")|"	// 11
                + "\\s+)";
        TOKENIZER = Pattern.compile(pattern);
    }

    public static ThreadSnapshotFilter parseFilter(String source, BasicFilterFactory factory) throws ParserException {
        FilterParser parser = new FilterParser(factory, source);
        return parser.parse();
    }

    public static PositionalStackMatcher parsePositionMatcher(String source, BasicFilterFactory factory) throws ParserException {
        FilterParser parser = new FilterParser(factory, source);
        return parser.parsePositionalMatcher();
    }


    private static class FilterParser {

        List<List<Op>> stackStash = new ArrayList<List<Op>>();
        List<Op> stack = new ArrayList<Op>();
        String text;
        Matcher matcher;
        int offset;
        BasicFilterFactory filterFactory;

        public FilterParser(BasicFilterFactory factory, String text) {
            this.text = text;
            this.filterFactory = factory;
            matcher = TOKENIZER.matcher(text);
        }

        public ThreadSnapshotFilter parse() {
            parseText();
            Op root = collapse();
            return produceFilter(root);
        }

        public PositionalStackMatcher parsePositionalMatcher() {
            parseText();
            Op root = collapse();
            return producePosFilter(root);
        }

        protected void parseText() {
            while(true) {
                if (matcher.lookingAt()) {
                    offset = matcher.start();
                    if (matcher.group(2) != null) {
                        processPar();
                    }
                    else if (matcher.group(3) != null) {
                        processPattern();
                    }
                    else if (matcher.group(4) != null) {
                        processOp(TokenType.COMMA, 3);
                    }
                    else if (matcher.group(5) != null) {
                        processOp(TokenType.PLUS, 1);
                    }
                    else if (matcher.group(6) != null) {
                        processOp(TokenType.EXCL, 1);
                    }
                    else if (matcher.group(7) != null) {
                        processOp(TokenType.SLASH_PLUS, 2);
                    }
                    else if (matcher.group(8) != null) {
                        processOp(TokenType.SLASH_EXCL, 2);
                    }
                    else if (matcher.group(9) != null) {
                        processOp(TokenType.SLASH_UP_PLUS, 2);
                    }
                    else if (matcher.group(10) != null) {
                        processOp(TokenType.SLASH_UP_EXCL, 2);
                    }
                    else if (matcher.group(11) != null) {
                        processStatePattern();
                    }
                }
                else {
                    throw error(matcher.regionStart(), "cannot parse");
                }

                if (matcher.end() == text.length()) {
                    break;
                }
                else {
                    matcher.region(matcher.end(), text.length());
                }
            }
        }

        private void processOp(TokenType tt, int rank) {
            Op op = new Op();
            op.toc = tt;
            op.rank = rank;
            op.body = matcher.group(1);
            op.offset = matcher.start();

            pushToken(op);
        }

        private void processPattern() {
            String pattern = matcher.group(1);
            Op op = new Op();
            op.toc = TokenType.PATTERN;
            op.rank = -1;
            op.body = pattern;
            op.offset = matcher.start();

            pushToken(op);
        }

        private void processUniverse() {
            String pattern = matcher.group(1);
            Op op = new Op();
            op.toc = TokenType.UNIVERSE;
            op.rank = -1;
            op.body = pattern;
            op.offset = matcher.start();

            pushToken(op);
        }

        private void processStatePattern() {
            String pattern = matcher.group(1);
            int off = "#STATE=".length();
            Op op = new Op();
            op.toc = TokenType.STATE_PATTERN;
            op.rank = -1;
            op.body = pattern.substring(off);
            op.offset = matcher.start() + off;

            pushToken(op);
        }

        private void processPar() {
            if (text.charAt(matcher.start()) == '(') {
                // opening paren
                stashStack();
            }
            else {
                if (stackStash.isEmpty()) {
                    error(offset, "No mathcing paranthesis");
                }
                // closing paren
                Op op = collapse();
                op.rank = -1;
                unstashStack();
                pushToken(op);
            }
        }

        private Op collapse() {

            if (stack.isEmpty()) {
                error(offset, "Empty expression");
            }
            if (stack.get(stack.size() - 1).rank > 0) {
                error(offset, "Incomplete operator");
            }

            while(stack.size() > 1) {
                mergeLastOp();
            }
            return stack.get(0);
        }

        private void unstashStack() {
            if (stackStash.size() < 0) {
                throw new RuntimeException("Nothing on stack");
            }
            stack = stackStash.remove(stackStash.size() - 1);
        }

        private void stashStack() {
            stackStash.add(stack);
            stack = new ArrayList<TraceFilterPredicateParser.Op>();
        }

        private Op last() {
            return stack.get(stack.size() - 1);
        }

        private void pushToken(Op op) {
            if (op.rank < 0) {
                if (stack.isEmpty()) {
                    stack.add(op);
                }
                else {
                    if (last().rank < 0) {
                        error(op.offset, " operator expected");
                    }
                    else {
                        stack.add(op);
                    }
                }
            }
            else if (op.rank >= 0) {
                if (stack.isEmpty()) {
                    if (op.toc == TokenType.EXCL) {
                        // special case
                        processUniverse();
                    }
                    else {
                        error(op.offset, " operator expected");
                    }
                }
                while(true) {
                    int lor = lastOpRank();
                    if (lor < 0 || lor < op.rank) {
                        stack.add(op);
                        break;
                    }
                    else {
                        mergeLastOp();
                        continue;
                    }
                }
            }
        }

        private int lastOpRank() {
            if (stack.size() == 0) {
                return -1;
            }
            else {
                int s = stack.size();
                Op op = stack.get(s - 1);
                if (op.rank >= 0) {
                    return op.rank;
                }
                else {
                    if (stack.size() < 2) {
                        return -1;
                    }
                    return stack.get(s - 2).rank;
                }
            }
        }

        private void mergeLastOp() {
            int s = stack.size();
            Op b = stack.remove(s - 1);
            Op o = stack.remove(s - 2);
            Op a = stack.remove(s - 3);
            if (o.rank < 0) {
                throw new RuntimeException("Op already collapsed");
            }
            o.left = a;
            o.right = b;
            o.rank = -1;
            stack.add(o);
        }

        private RuntimeException error(int offs, String message) {
            throw new ParserException(text, offs, message);
        }

        private ThreadSnapshotFilter produceFilter(Op node) {
            switch(node.toc) {
                case PATTERN:
                    return filterFactory.frameFilter(filterFactory.patternFrameMatcher(refinePattern(node.body)));
                case STATE_PATTERN:
                    return filterFactory.threadStateMatter(node.body);
                case COMMA:
                    return produceConjunctionFilter(node);
                case PLUS:
                    return filterFactory.disjunction(produceFilter(node.left), produceFilter(node.right));
                case EXCL:
                    return filterFactory.disjunction(produceFilter(node.left), filterFactory.not(produceFilter(node.right)));
                case SLASH_PLUS:
                    return filterFactory.followed(filterFactory.lastFrame(produceMatcher(node.left)), produceFilter(node.right));
                case SLASH_EXCL:
                    return filterFactory.followed(filterFactory.lastFrame(produceMatcher(node.left)), filterFactory.not(produceFilter(node.right)));
                case SLASH_UP_PLUS:
                    return filterFactory.followed(filterFactory.firstFrame(produceMatcher(node.left)), produceFilter(node.right));
                case SLASH_UP_EXCL:
                    return filterFactory.followed(filterFactory.firstFrame(produceMatcher(node.left)), filterFactory.not(produceFilter(node.right)));
                case UNIVERSE:
                    return filterFactory.trueFilter();
                default:
                    throw new RuntimeException("Unknown node");
            }
        }

        private PositionalStackMatcher producePosFilter(Op node) {
            switch(node.toc) {
                case PATTERN:
                case COMMA:
                    return (PositionalStackMatcher)filterFactory.followed(filterFactory.firstFrame(produceMatcher(node)), filterFactory.trueFilter());
                case SLASH_PLUS:
                    return (PositionalStackMatcher)filterFactory.followed(filterFactory.lastFrame(produceMatcher(node.left)), produceFilter(node.right));
                case SLASH_EXCL:
                    return (PositionalStackMatcher)filterFactory.followed(filterFactory.lastFrame(produceMatcher(node.left)), filterFactory.not(produceFilter(node.right)));
                case SLASH_UP_PLUS:
                    return (PositionalStackMatcher)filterFactory.followed(filterFactory.firstFrame(produceMatcher(node.left)), produceFilter(node.right));
                case SLASH_UP_EXCL:
                    return (PositionalStackMatcher)filterFactory.followed(filterFactory.firstFrame(produceMatcher(node.left)), filterFactory.not(produceFilter(node.right)));
                default:
                    throw new RuntimeException("Positional operator required");
            }
        }

        /**
         * Optimize conjunction into single frame matcher where possible.
         */
        private ThreadSnapshotFilter produceConjunctionFilter(Op node) {
            List<String> pattern = new ArrayList<String>();
            while(node.toc == TokenType.COMMA && node.right.toc == TokenType.PATTERN) {
                pattern.add(refinePattern(node.right.body));
                node = node.left;
            }

            if (pattern.isEmpty()) {
                return filterFactory.conjunction(produceFilter(node.left), produceFilter(node.right));
            }
            else {
                if (node.toc == TokenType.PATTERN) {
                    pattern.add(refinePattern(node.body));
                    node = null;
                }
                ThreadSnapshotFilter f = filterFactory.frameFilter(filterFactory.patternFrameMatcher(pattern));
                return node == null ? f : filterFactory.conjunction(f, produceFilter(node));
            }
        }

        /**
         * Optimize conjunction into single frame matcher where possible.
         */
        private StackFrameMatcher produceConjunctionMatcher(Op node) {
            List<String> pattern = new ArrayList<String>();
            while(node.toc == TokenType.COMMA && node.right.toc == TokenType.PATTERN) {
                pattern.add(refinePattern(node.right.body));
                node = node.left;
            }

            if (pattern.isEmpty()) {
                return filterFactory.matcherConjunction(produceMatcher(node), produceMatcher(node.right));
            }
            else {
                if (node.toc == TokenType.PATTERN) {
                    pattern.add(refinePattern(node.body));
                    node = null;
                }
                StackFrameMatcher f = filterFactory.patternFrameMatcher(pattern);
                return node == null ? f : filterFactory.matcherConjunction(f, produceMatcher(node));
            }
        }

        private String refinePattern(String body) {
            // in stack trace ':' may appear only after filename.java
            // replace '*:' with '*.*:' to simplify line number patterns
            int c = body.lastIndexOf(':');
            if (c > 0) {
                if (c > 2) {
                    if (body.charAt(c - 1) == '*' && body.charAt(c - 2) == '*') {
                        // **: - is ok
                        return body;
                    }
                }
                if (c > 1 && body.charAt(c - 1) == '*') {
                    return body.substring(0, c - 1) + "*.*:" + body.substring(c + 1);
                }
            }
            return body;
        }

        private StackFrameMatcher produceMatcher(Op node) {
            switch(node.toc) {
                case PATTERN:
                    return filterFactory.patternFrameMatcher(refinePattern(node.body));
                case STATE_PATTERN:
                    throw error(node.offset, "Unsupported for frame predicate");
                case COMMA:
                    return produceConjunctionMatcher(node);
                case PLUS:
                    throw error(node.offset, "Unsupported for frame predicate");
                case EXCL:
                    throw error(node.offset, "Unsupported for frame predicate");
                case SLASH_PLUS:
                    throw error(node.offset, "Unsupported for frame predicate");
                case SLASH_EXCL:
                    throw error(node.offset, "Unsupported for frame predicate");
                case SLASH_UP_PLUS:
                    throw error(node.offset, "Unsupported for frame predicate");
                case SLASH_UP_EXCL:
                    throw error(node.offset, "Unsupported for frame predicate");
                case UNIVERSE:
                    return filterFactory.patternFrameMatcher("**");
                default:
                    throw new RuntimeException("Unknown node");
            }
        }
    }

    private static enum TokenType {
        UNIVERSE,
        PATTERN,
        STATE_PATTERN,
        COMMA,
        PLUS,
        EXCL,
        SLASH_PLUS,
        SLASH_EXCL,
        SLASH_UP_PLUS,
        SLASH_UP_EXCL,
    }

    private static class Op {

        TokenType toc;
        int rank;

        int offset;
        String body;

        Op left;
        Op right;

        public String toString() {
            if (left == null && right == null) {
                return "'" + body + "'";
            }
            else {
                return "'" + body + "'" + "(" + left + ", " + right + ")";
            }
        }
    }
}
