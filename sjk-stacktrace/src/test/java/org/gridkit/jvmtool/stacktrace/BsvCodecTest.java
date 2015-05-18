package org.gridkit.jvmtool.stacktrace;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import org.junit.Test;

public class BsvCodecTest {

    ByteArrayOutputStream bos = new ByteArrayOutputStream();
    FlatSampleWriter writer;
    
    private FlatSampleWriter writer() throws IOException {
        writer = BsvCodec.createWriter(bos);
        return writer;
    }

    private FlatSampleReader reader() throws IOException {
        writer.close();
        byte[] data = bos.toByteArray();
        bos = new ByteArrayOutputStream();
        ByteArrayInputStream bis = new ByteArrayInputStream(data);
        return BsvCodec.createReader(bis);
    }
    
    @Test
    public void test_simple_values() throws IOException {
        FlatSampleWriter writer = writer();
        
        writer.set("A", null);
        writer.set("B", 10);
        writer.set("C", 1.1d);
        writer.set("D", "abc");
        writer.set("E", Long.MAX_VALUE);
        writer.set("F", -1);
        writer.set("H", 1l << 50);
        writer.push();

        writer.set("A", null);
        writer.set("B", 10);
        writer.set("C", 1.1d);
        writer.set("D", "abc");
        writer.set("E", Long.MAX_VALUE);
        writer.set("F", -1);
        writer.set("H", 1l << 48);
        writer.set("I", -100);
        writer.push();
        
        FlatSampleReader reader = reader();
        
        reader.prime();
        
        assertEquals(asList("A", "B", "C", "D", "E", "F", "H"), reader.getAllFields());
        assertEquals(null, reader.get("A"));
        assertEquals(10l, reader.get("B"));
        assertEquals(1.1d, reader.get("C"));
        assertEquals("abc", reader.get("D"));
        assertEquals(Long.MAX_VALUE, reader.get("E"));
        assertEquals(-1l, reader.get("F"));
        assertEquals(1l << 50, reader.get("H"));
        
        assertTrue(reader.advance());
        
        assertEquals(asList("A", "B", "C", "D", "E", "F", "H", "I"), reader.getAllFields());
        assertEquals(null, reader.get("A"));
        assertEquals(10l, reader.get("B"));
        assertEquals(1.1d, reader.get("C"));
        assertEquals("abc", reader.get("D"));
        assertEquals(Long.MAX_VALUE, reader.get("E"));
        assertEquals(-1l, reader.get("F"));
        assertEquals(1l << 48, reader.get("H"));
        assertEquals(-100l, reader.get("I"));

    }
    
}
