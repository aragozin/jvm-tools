package org.gridkit.jvmtool.jackson;

import org.junit.Assert;
import org.junit.Test;

public class NumberOutputTest {

    @Test
    public void testOutputIntCharArray() {
        char[] c = new char[]
                {'1', '2', '3', '4', '5', '6', '7', '8', '9', '0', '1'};

        Assert.assertEquals(2, NumberOutput.outputInt(-1, c, 0));
        Assert.assertEquals(2, NumberOutput.outputInt(15, c, 0));
        Assert.assertEquals(4, NumberOutput.outputInt(2000, c, 0));
        Assert.assertEquals(7, NumberOutput.outputInt(1000001, c, 0));
        Assert.assertEquals(10, NumberOutput.outputInt(1000000001, c, 0));
        Assert.assertEquals(10, NumberOutput.outputInt(2000000001, c, 0));
        Assert.assertEquals(11, NumberOutput.outputInt(-2147483648, c, 0));
    }

    @Test
    public void testOutputIntByteArray() {
        byte[] bytes = new byte[]{1, 2, 3, 4, 5, 6, 7, 8, 9, 0, 1};

        Assert.assertEquals(2, NumberOutput.outputInt(-1, bytes, 0));
        Assert.assertEquals(2, NumberOutput.outputInt(15, bytes, 0));
        Assert.assertEquals(4, NumberOutput.outputInt(2000, bytes, 0));
        Assert.assertEquals(7, NumberOutput.outputInt(1000001, bytes, 0));
        Assert.assertEquals(10, NumberOutput.outputInt(1000000001, bytes, 0));
        Assert.assertEquals(10, NumberOutput.outputInt(2000000001, bytes, 0));
        Assert.assertEquals(11, NumberOutput.outputInt(-2147483648, bytes, 0));
    }

    @Test
    public void testOutputLongCharArray() {
        char[] c = new char[]
                {'1', '2', '3', '4', '5', '6', '7', '8', '9', '0', '1'};

        Assert.assertEquals(2, NumberOutput.outputLong(-1L, c, 0));
        Assert.assertEquals(10, NumberOutput.outputLong(2147483647L, c, 0));
        Assert.assertEquals(11, NumberOutput.outputLong(10000000001L, c, 0));
        Assert.assertEquals(11, NumberOutput.outputLong(-2147483648L, c, 0));
    }

    @Test
    public void testOutputLongByteArray() {
        byte[] bytes = new byte[]{1, 2, 3, 4, 5, 6, 7, 8, 9, 0, 1};

        Assert.assertEquals(2, NumberOutput.outputLong(-1L, bytes, 0));
        Assert.assertEquals(10, NumberOutput.outputLong(2147483647L, bytes, 0));
        Assert.assertEquals(11, NumberOutput.outputLong(10000000001L, bytes, 0));
        Assert.assertEquals(11, NumberOutput.outputLong(-2147483648L, bytes, 0));
    }

    @Test
    public void testToString() {
        Assert.assertEquals("3", NumberOutput.toString(3));
        Assert.assertEquals("-1", NumberOutput.toString(-1));
        Assert.assertEquals("20", NumberOutput.toString(20));
        Assert.assertEquals("20", NumberOutput.toString(20L));
        Assert.assertEquals("2.52", NumberOutput.toString(2.52));
        Assert.assertEquals("2147483648", NumberOutput.toString(2147483648L));
        Assert.assertEquals("-2147483649",
                NumberOutput.toString(-2147483649L));
    }
}
