package org.gridkit.jvmtool;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.Assert;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;

public class ConsoleRule extends TestWatcher {

    public static ConsoleRule out() {
        return new ConsoleRule(false);
    }

    public static ConsoleRule err() {
        return new ConsoleRule(true);
    }

    final boolean err;

    ByteArrayOutputStream std = new ByteArrayOutputStream();

    PrintStream orig;

    List<ConsoleMatcher> matchers = new ArrayList<ConsoleMatcher>();

    ConsoleRule(boolean err) {
        this.err = err;
    }

    @Override
    protected void starting(Description description) {
        super.starting(description);
        orig = err ? System.err : System.out;
        PrintStream tee = new PrintStream(new TeeOutputStream(orig, std));
        if (err) {
            System.setErr(tee);
        } else {
            System.setOut(tee);
        }
    }

    @Override
    protected void finished(Description description) {
        super.finished(description);
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

        StringBuilder sb = new StringBuilder();
        for (ConsoleMatcher cm: cms) {
            sb.append("(" + RegExHelper.uncapture(cm.getLineMatcher() + ")"));
        }
        Pattern pp = Pattern.compile(sb.toString());

        String text = new String(std.toByteArray());
        text = text.replace((CharSequence)"\r\n", "\n");
        std.reset();

        Matcher mm = pp.matcher(text);
        if (!mm.lookingAt()) {
            Assert.fail("Console content does not match");
        }
        else {
            for (int i = 0; i != cms.length; ++i) {
                String match = mm.group(i + 1);
                cms[i].verifyMatch(match);
            }
        }
        text = text.substring(mm.end());
        if (text.length() > 0) {
            try {
                std.write(text.getBytes());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public ConsoleRule skip() {
        matchers.add(new SkipMatcher());
        return this;
    }

    public ConsoleRule skip(int lines) {
        for (int i = 0; i != lines; ++i) {
            lineEx("[^\\n]*");
        }
        return this;
    }


    public ConsoleRule line(String exact) {
        matchers.add(new LineMatcher(Pattern.quote(exact), new String[0]));
        return this;
    }

    public ConsoleRule lineStarts(String starts) {
        return lineStartsEx(Pattern.quote(starts), new String[0]);
    }

    public ConsoleRule lineStartsEx(String starts, String... vars) {
        String pattern = starts + "[^\\n]*";
        Pattern.compile(pattern);
        matchers.add(new LineMatcher(pattern, vars));
        return this;
    }

    public ConsoleRule lineContains(String substring) {
        return lineContainsEx(Pattern.quote(substring), new String[0]);
    }

    public ConsoleRule lineContainsEx(String substring, String... vars) {
        String pattern = "[^\\n]*" + substring + "[^\\n]*";
        Pattern.compile(pattern);
        matchers.add(new LineMatcher(pattern, vars));
        return this;
    }

    public ConsoleRule lineEx(String pattern, String... vars) {
        Pattern.compile(pattern);
        matchers.add(new LineMatcher(pattern, vars));
        return this;
    }

    private abstract class ConsoleMatcher {

        public abstract String getLineMatcher();

        public abstract void verifyMatch(String match);

    }

    private class SkipMatcher extends ConsoleMatcher {

        @Override
        public String getLineMatcher() {
            return "([^\\n]*\\n)*";
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
