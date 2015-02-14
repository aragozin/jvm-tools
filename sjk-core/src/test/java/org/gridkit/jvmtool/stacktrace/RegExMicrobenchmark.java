package org.gridkit.jvmtool.stacktrace;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.gridkit.jvmtool.GlobHelper;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;

import com.carrotsearch.junitbenchmarks.BenchmarkRule;

public class RegExMicrobenchmark {

    private static StackFrame[] FRAMES;
    private static StackFrame[] INTERNED_FRAMES;
    private static StackTraceElement[] TRACE_ELEMENTS;
    static {
        FRAMES = loadFrames("stack-frame.txt", 1000);
        INTERNED_FRAMES = new StackFrame[FRAMES.length];
        TRACE_ELEMENTS = new StackTraceElement[FRAMES.length];
        for(int i = 0; i != FRAMES.length; ++i) {
            INTERNED_FRAMES[i] = FRAMES[i].internSymbols();
            TRACE_ELEMENTS[i] = FRAMES[i].toStackTraceElement();
        }
    }

    private static Pattern pattern1 = GlobHelper.translate("org.hibernate.type.AbstractStandardBasicType.getHashCode", ".");
    private static Pattern pattern2 = GlobHelper.translate("**.TagMethodExpression.invoke", ".");
    private static Pattern pattern3 = GlobHelper.translate("org.jboss.seam.**", ".");

    private static StackFrame[] loadFrames(String res, int multiplier) {
        try {
            List<StackFrame> frames = new ArrayList<StackFrame>();
            for(int i = 0; i != multiplier; ++i) {
                InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream(res);
                BufferedReader br = new BufferedReader(new InputStreamReader(is));
                while(true) {
                    String line = br.readLine();
                    if (line == null) {
                        break;
                    }
                    if (line.trim().length() > 0)
                    frames.add(StackFrame.parseTrace(line.trim()));
                }
            }
            return frames.toArray(new StackFrame[frames.size()]);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    };

    @Rule
    public TestName testName = new TestName();

    @Rule
    public volatile BenchmarkRule benchmark = new BenchmarkRule();

    @Test
    public void frame_pattern1() {
        testFrameMatcher(FRAMES, pattern1);
    }

    @Test
    public void frame_pattern2() {
        testFrameMatcher(FRAMES, pattern2);
    }

    @Test
    public void frame_pattern3() {
        testFrameMatcher(FRAMES, pattern3);
    }

    @Test
    public void frame_patternAll() {
        testFrameMatcher(FRAMES, pattern1, pattern2, pattern3);
    }

    @Test
    public void interned_frame_pattern1() {
        testFrameMatcher(INTERNED_FRAMES, pattern1);
    }

    @Test
    public void interned_frame_pattern2() {
        testFrameMatcher(INTERNED_FRAMES, pattern2);
    }

    @Test
    public void interned_frame_pattern3() {
        testFrameMatcher(INTERNED_FRAMES, pattern3);
    }

    @Test
    public void interned_frame_patternAll() {
        testFrameMatcher(INTERNED_FRAMES, pattern1, pattern2, pattern3);
    }

    @Test
    public void trace_element_pattern1() {
        testFrameMatcher(TRACE_ELEMENTS, pattern1);
    }

    @Test
    public void trace_element_pattern2() {
        testFrameMatcher(TRACE_ELEMENTS, pattern2);
    }

    @Test
    public void trace_element_pattern3() {
        testFrameMatcher(TRACE_ELEMENTS, pattern3);
    }

    @Test
    public void trace_element_patternAll() {
        testFrameMatcher(TRACE_ELEMENTS, pattern1, pattern2, pattern3);
    }

    protected void testFrameMatcher(StackFrame[] frames, Pattern... pattern) {
        int[] count = new int[pattern.length];
        Matcher[] matcher = new Matcher[pattern.length];
        for(int i = 0; i != pattern.length; ++i) {
            matcher[i] = pattern[i].matcher("");
        }
        for(StackFrame frame: frames) {
            for(int i = 0; i != pattern.length; ++i) {
                if (match(frame, matcher[i])) {
                    ++count[i];
                }
            }
        }
        if (benchmark == null) {
            System.out.println(testName.getMethodName());
            for(int i = 0; i != pattern.length; ++i) {
                System.out.println("    " + count[i] + " - " + pattern[i].pattern());
            }
        }
    }

    protected void testFrameMatcher(StackTraceElement[] frames, Pattern... pattern) {
        int[] count = new int[pattern.length];
        Matcher[] matcher = new Matcher[pattern.length];
        for(int i = 0; i != pattern.length; ++i) {
            matcher[i] = pattern[i].matcher("");
        }
        for(StackTraceElement frame: frames) {
            for(int i = 0; i != pattern.length; ++i) {
                if (match(frame.toString(), matcher[i])) {
                    ++count[i];
                }
            }
        }
        if (benchmark == null) {
            System.out.println(testName.getMethodName());
            for(int i = 0; i != pattern.length; ++i) {
                System.out.println("    " + count[i] + " - " + pattern[i].pattern());
            }
        }
    }

    private boolean match(CharSequence frame, Matcher pattern) {
        pattern.reset(frame);
        return pattern.lookingAt();
    }

}
