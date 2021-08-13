package org.gridkit.sjk.test.console;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.logging.log4j.util.Supplier;
import org.gridkit.sjk.test.console.PatternMatcher.AnyLinesNode;
import org.gridkit.sjk.test.console.PatternMatcher.LineMatcherNode;
import org.gridkit.sjk.test.console.PatternMatcher.MatcherNode;
import org.gridkit.sjk.test.console.PatternMatcher.PatternNode;
import org.gridkit.sjk.test.console.PatternMatcher.SequenceNode;

public class ConsoleTracker {

    public static ConsoleTracker out() {
        return new ConsoleTracker(false);
    }

    public static ConsoleTracker err() {
        return new ConsoleTracker(true);
    }

    final boolean err;

    ByteArrayOutputStream buffer = new ByteArrayOutputStream();

    PrintStream orig;

    List<ConsoleMatcher> matchers = new ArrayList<ConsoleMatcher>();

    ConsoleTracker(boolean err) {
        this.err = err;
    }

    public void init() {
        orig = err ? System.err : System.out;
        PrintStream tee = new PrintStream(new TeeOutputStream(orig, buffer));
        if (err) {
            System.setErr(tee);
        } else {
            System.setOut(tee);
        }
    }

    public void complete() {
        verify();
        if (err) {
            System.setErr(orig);
        } else {
            System.setOut(orig);
        }
    }

    public void clean() {
        buffer.reset();
    }

    /**
     * Repeat console matching retries while <code>until</code> return false.
     * @throws InterruptedException
     */
    public void waitForMatch(Supplier<Boolean> until) throws InterruptedException {
        while(true) {
            if (tryMatch()) {
                return;
            } else {
                if (until.get()) {
                    break;
                } else {
                    Thread.sleep(100);
                }
            }
        }
        verify();
    }

    /**
     * Verify console pattern match without raising {@link AssertionError}.
     */
    public boolean tryMatch() {
        if (err) {
            System.err.flush();
        } else {
            System.out.flush();
        }
        if (matchers.isEmpty()) {
            return true;
        }
        ConsoleMatcher[] cms = matchers.toArray(new ConsoleMatcher[0]);
        matchers.clear();

        String text = getText();
        buffer.reset();

        PatternNode node = compile(cms);

        PatternMatcher matcher = new PatternMatcher(node);

        int ln = matcher.matchStart(text);
        boolean matched = false;
        if (ln < 0) {
            matched = true;
        }

        List<String> lines = matcher.lines();

        // push unmatched lines back to buffer
        for (int i = 0; i < lines.size(); ++i) {
            try {
                buffer.write((lines.get(i) + "\n").getBytes());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        return matched;
    }

    public void verify() {
        if (err) {
            System.err.flush();
        } else {
            System.out.flush();
        }
        if (matchers.isEmpty()) {
            return;
        }
        ConsoleMatcher[] cms = matchers.toArray(new ConsoleMatcher[0]);
        matchers.clear();

        String text = getText();
        buffer.reset();

        PatternNode node = compile(cms);

        PatternMatcher matcher = new PatternMatcher(node);

        int ln = matcher.matchStart(text);
        if (ln < 0) {
            System.err.println(matcher.reportMatchProblems());
            Assert.fail("Console content does not match");
        }

        List<String> lines = matcher.lines();

        // push unmatched lines back to buffer
        for (int i = ln; i < lines.size(); ++i) {
            try {
                buffer.write((lines.get(i) + "\n").getBytes());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private PatternNode compile(ConsoleMatcher[] cms) {
        PatternNode nodes[] = new PatternNode[cms.length];
        for (int i = 0; i != cms.length; ++i) {
            nodes[i] = cms[i].getMatcherNode();
        }
        return new SequenceNode(nodes);
    }

    private String getText() {
        String text = new String(buffer.toByteArray());
        text = text.replace((CharSequence)"\r\n", "\n");
        return text;
    }

    public ConsoleTracker skip() {
        matchers.add(new SkipMatcher(0, -1));
        return this;
    }

    public ConsoleTracker skip(int lines) {
        matchers.add(new SkipMatcher(lines, lines));
        return this;
    }

    public ConsoleTracker skipMax(int lines) {
        matchers.add(new SkipMatcher(0, lines));
        return this;
    }

    public ConsoleTracker line(String exact) {
        matchers.add(new LineMatcher(Pattern.quote(exact), new String[0]));
        return this;
    }

    public ConsoleTracker lineStarts(String starts) {
        return lineStartsEx(Pattern.quote(starts), new String[0]);
    }

    public ConsoleTracker lineStartsEx(String starts, String... vars) {
        String pattern = starts + ".*";
        Pattern.compile(pattern);
        matchers.add(new LineMatcher(pattern, vars));
        return this;
    }

    public ConsoleTracker lineContains(String... substrings) {
        if (substrings.length == 0) {
            throw new IllegalArgumentException("At least one string is required");
        } else if (substrings.length == 1) {
            return lineContainsEx(".*" + Pattern.quote(substrings[0]) + ".*");
        } else {
            matchers.add(new MultiContainsMatcher(substrings));
            return this;
        }
    }

    public ConsoleTracker lineContainsEx(String substring, String... vars) {
        String pattern = substring;
        Pattern.compile(pattern);
        matchers.add(new LineMatcher(pattern, vars));
        return this;
    }

    public ConsoleTracker lineEx(String pattern, String... vars) {
        Pattern.compile(pattern);
        matchers.add(new LineMatcher(pattern, vars));
        return this;
    }

    private abstract class ConsoleMatcher {

        public abstract PatternNode getMatcherNode();

        public abstract void verifyMatch(String match);

    }

    private class SkipMatcher extends ConsoleMatcher {

        private int minMatches;
        private int maxMatches;

        public SkipMatcher(int minMatches, int maxMatches) {
            this.minMatches = minMatches;
            this.maxMatches = maxMatches;
        }

        @Override
        public PatternNode getMatcherNode() {
            return new AnyLinesNode(minMatches, maxMatches);
        }

        @Override
        public void verifyMatch(String match) {
            // do nothing
        }
    }

    private class MultiContainsMatcher extends ConsoleMatcher {

        private final String[] substrings;

        public MultiContainsMatcher(String[] substrings) {
            this.substrings = substrings;
        }

        @Override
        public PatternNode getMatcherNode() {
            return new MatcherNode(new PatternMatcher.LineMatcher() {

                @Override
                public boolean match(String line) {
                    for (String s: substrings) {
                        if (!line.contains(s)) {
                            return false;
                        }
                    }
                    return true;
                }

                @Override
                public String toString() {
                    return MultiContainsMatcher.this.toString();
                }
            });
        }

        @Override
        public void verifyMatch(String match) {
        }

        @Override
        public String toString() {
            return "AllSubstrings" + Arrays.toString(substrings);
        }
    }

    private class LineMatcher extends ConsoleMatcher {

        private final String pattern;
        private final String[] placeholders;

        public LineMatcher(String pattern, String[] placeholders) {
            this.pattern = pattern;
            this.placeholders = placeholders;
        }

        @Override
        public PatternNode getMatcherNode() {
            return new LineMatcherNode() {

                @Override
                public boolean match(String line) {
                    Matcher m = Pattern.compile(pattern).matcher(line);
                    if (!m.matches()) {
                        return false;
                    }

                    if (placeholders != null) {
                        List<String> missmatches = new ArrayList<String>();
                        for (int i = 0; i != placeholders.length; ++i) {
                            if (placeholders[i] != null && !placeholders[i].equals(m.group(i + 1))) {
                                missmatches.add(placeholders[i] + " <> " + m.group(i + 1));
                            }
                        }
                        if (!missmatches.isEmpty()) {
                            return false;
                        }
                    }
                    return true;
                }

                @Override
                public String toString() {
                    return "RegEx[" + pattern + "]";
                }
            };
        }

        @Override
        public void verifyMatch(String match) {
            if (match.endsWith("\r\n")) {
                match = match.substring(0, match.length() -2);
            } else if (match.endsWith("\n") || match.endsWith("\r")) {
                match = match.substring(0, match.length() -1);
            }

            Matcher m = Pattern.compile(pattern).matcher(match);
            if (!m.matches()) {
                Assert.fail("Line does not match\nExpect: " + pattern + "\nLine: " + match);
            }

            if (placeholders != null) {
                List<String> missmatches = new ArrayList<String>();
                for (int i = 0; i != placeholders.length; ++i) {
                    if (placeholders[i] != null && !placeholders[i].equals(m.group(i + 1))) {
                        missmatches.add(placeholders[i] + " <> " + m.group(i + 1));
                    }
                }
                if (!missmatches.isEmpty()) {
                    StringBuilder sb = new StringBuilder();
                    sb.append("Line placeholders missmatch");
                    for (String mm: missmatches) {
                        sb.append("\n").append(mm);
                    }
                    Assert.fail(sb.toString());
                }
            }
        }
    }

    @Override
    public String toString() {
        return getText();
    }

    private static class TeeOutputStream extends OutputStream {

        private final OutputStream a;
        private final OutputStream b;

        public TeeOutputStream(OutputStream a, OutputStream b) {
            this.a = a;
            this.b = b;
        }

        @Override
        public void write(int d) throws IOException {
            a.write(d);
            b.write(d);
        }

        @Override
        public void write(byte[] d) throws IOException {
            a.write(d);
            b.write(d);
        }

        @Override
        public void write(byte[] d, int off, int len) throws IOException {
            a.write(d, off, len);
            b.write(d, off, len);
        }

        @Override
        public void flush() throws IOException {
            a.flush();
            b.flush();
        }
    }
}
