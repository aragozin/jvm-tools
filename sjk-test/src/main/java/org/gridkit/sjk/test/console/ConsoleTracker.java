package org.gridkit.sjk.test.console;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.gridkit.sjk.test.console.PatternMatcher.*;
import org.junit.Assert;

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
        matchers.add(new SkipMatcher());
        return this;
    }

    public ConsoleTracker skip(int lines) {
        for (int i = 0; i != lines; ++i) {
            lineEx(".*");
        }
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
            StringBuilder sb = new StringBuilder();
            sb.append("(?:.*(?:");
            for (String ss: substrings) {
                sb.append(Pattern.quote(ss)).append("|");
            }
            sb.setLength(sb.length() - 1);
            sb.append(").*){" + substrings.length + "}");
            return lineContainsEx(sb.toString());
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

        public abstract String getLineMatcher();

        public abstract PatternNode getMatcherNode();

        public abstract void verifyMatch(String match);

    }

    private class SkipMatcher extends ConsoleMatcher {

        @Override
        public String getLineMatcher() {
            return "([^\\n]*\\n)*";
        }

        @Override
        public PatternNode getMatcherNode() {
            return new AnyLinesNode();
        }

        @Override
        public void verifyMatch(String match) {
            // do nothing
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
        public String getLineMatcher() {
            return pattern + "\\n";
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
                    return "\"" + pattern + "\"";
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
