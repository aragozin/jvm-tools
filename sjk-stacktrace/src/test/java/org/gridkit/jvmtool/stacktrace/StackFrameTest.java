package org.gridkit.jvmtool.stacktrace;

import java.lang.reflect.Field;

import org.junit.Assert;
import org.junit.Test;

public class StackFrameTest {

    @Test
    public void test_normal_frame_at_1() {
        StackTraceElement ste = new StackTraceElement("MyClass", "myMethod", "MyClass.java", 1);
        StackFrame frame = new StackFrame(ste);

        Assert.assertEquals(ste.toString(), frame.toString());
    }

    @Test
    public void test_normal_frame_at_5() {
        StackTraceElement ste = new StackTraceElement("MyClass", "myMethod", "MyClass.java", 5);
        StackFrame frame = new StackFrame(ste);

        Assert.assertEquals(ste.toString(), frame.toString());
    }
    @Test
    public void test_normal_frame_at_10() {
        StackTraceElement ste = new StackTraceElement("MyClass", "myMethod", "MyClass.java", 10);
        StackFrame frame = new StackFrame(ste);

        Assert.assertEquals(ste.toString(), frame.toString());
    }

    @Test
    public void test_normal_frame_at_15() {
        StackTraceElement ste = new StackTraceElement("MyClass", "myMethod", "MyClass.java", 15);
        StackFrame frame = new StackFrame(ste);

        Assert.assertEquals(ste.toString(), frame.toString());
    }

    @Test
    public void test_normal_frame_at_100() {
        StackTraceElement ste = new StackTraceElement("MyClass", "myMethod", "MyClass.java", 100);
        StackFrame frame = new StackFrame(ste);

        Assert.assertEquals(ste.toString(), frame.toString());
    }

    @Test
    public void test_normal_frame_at_500() {
        StackTraceElement ste = new StackTraceElement("mypackage.MyClass", "myMethod", "MyClass.java", 500);
        StackFrame frame = new StackFrame(ste);

        Assert.assertEquals(ste.toString(), frame.toString());
    }

    @Test
    public void test_no_line_frame() {
        StackTraceElement ste = new StackTraceElement("MyClass", "myMethod", "MyClass.java", -1);
        StackFrame frame = new StackFrame(ste);

        Assert.assertEquals(ste.toString(), frame.toString());
    }

    @Test
    public void test_no_source_frame() {
        StackTraceElement ste = new StackTraceElement("MyClass", "myMethod", null, -1);
        StackFrame frame = new StackFrame(ste);

        Assert.assertEquals(ste.toString(), frame.toString());
    }

    @Test
    public void test_native_frame() {
        StackTraceElement ste = new StackTraceElement("MyClass", "myMethod", null, -2);
        StackFrame frame = new StackFrame(ste);

        Assert.assertEquals(ste.toString(), frame.toString());
    }

    @Test
    public void test_native_frame2() {
        StackTraceElement ste = new StackTraceElement("MyClass", "myMethod", "myclass.cpp", -2);
        StackFrame frame = new StackFrame(ste);

        Assert.assertEquals(ste.toString(), frame.toString());
    }

    @Test
    public void test_class_prefix() {
        StackTraceElement ste = new StackTraceElement("mypackage.MyClass", "myMethod", "MyClass.java", 15);
        StackFrame frame = new StackFrame("mypackage", "MyClass", "myMethod", "MyClass.java", 15);

        Assert.assertEquals(ste.toString(), frame.toString());
    }

    @Test
    public void test_simple_equality() {
        StackFrame frame1 = new StackFrame("mypackage", "MyClass", "myMethod", "MyClass.java", 15);
        StackFrame frame2 = new StackFrame("mypackage", "MyClass", "myMethod", "MyClass.java", 15);

        Assert.assertEquals(frame1, frame2);
        Assert.assertEquals(frame2, frame1);
        Assert.assertEquals(frame1.hashCode(), frame2.hashCode());
    }

    @Test
    public void test_equality_with_prefix_mismatch() {
        StackFrame frame1 = new StackFrame(null, "mypackage.MyClass", "myMethod", "MyClass.java", 15);
        StackFrame frame2 = new StackFrame("mypackage", "MyClass", "myMethod", "MyClass.java", 15);

        Assert.assertEquals(frame1, frame2);
        Assert.assertEquals(frame2, frame1);
        Assert.assertEquals(frame1.hashCode(), frame2.hashCode());
    }

    @Test
    public void test_non_equality_with_prefix_mismatch() {
        StackFrame frame1 = new StackFrame(null, "mypackage$MyClass", "myMethod", "MyClass.java", 15);
        StackFrame frame2 = new StackFrame("mypackage", "MyClass", "myMethod", "MyClass.java", 15);

        Assert.assertNotEquals(frame1.hashCode(), frame2.hashCode());

        zeroHash(frame1);
        zeroHash(frame2);

        Assert.assertFalse(frame1.equals(frame2));
        Assert.assertFalse(frame2.equals(frame1));
    }

    private static void zeroHash(StackFrame frame) {
        try {
            Field f = StackFrame.class.getDeclaredField("hash");
            f.setAccessible(true);
            f.set(frame, 0);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
