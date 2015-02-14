package org.gridkit.jvmtool.stacktrace;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;

import org.gridkit.jvmtool.StackTraceCodec;
import org.gridkit.jvmtool.StackTraceCodec.StackTraceWriter;
import org.gridkit.jvmtool.StackTraceReader;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;

public class StateTraceReaderTest {

    @Rule
    public TestName testName = new TestName();

    @Test
    public void read_dump_v1() throws FileNotFoundException, IOException {

        StackTraceReader reader = StackTraceCodec.newReader(new FileInputStream("src/test/resources/dump_v1.std"));

        if (!reader.isLoaded()) {
            reader.loadNext();
        }

        int n = 0;
        while(reader.isLoaded()) {
            reader.loadNext();
            ++n;
        }

        System.out.println("Read " + n + " traces from file");
    }

    @Test
    public void read_dump_v1_rewrite_and_compare() throws FileNotFoundException, IOException {

        File file = new File("target/tmp/" + testName.getMethodName() + "-" + System.currentTimeMillis() + ".std");
        file.getParentFile().mkdirs();
        file.delete();

        FileOutputStream fow = new FileOutputStream(file);
        StackTraceWriter writer = StackTraceCodec.newWriter(fow);

        StackTraceReader reader = StackTraceCodec.newReader(new FileInputStream("src/test/resources/dump_v1.std"));

        copyAllTraces(reader, writer);
        writer.close();

        reader = StackTraceCodec.newReader(new FileInputStream(file));
        StackTraceReader origReader = StackTraceCodec.newReader(new FileInputStream("src/test/resources/dump_v1.std"));

        assertEqual(origReader, reader);
    }


    void assertEqual(StackTraceReader r1, StackTraceReader r2) throws IOException {
        if (!r1.isLoaded()) {
            r1.loadNext();
        }
        if (!r2.isLoaded()) {
            r2.loadNext();
        }

        int n = 0;
        while(r1.isLoaded() && r2.isLoaded()) {
            assertSameState(r1, r2);
            r1.loadNext();
            r2.loadNext();
            ++n;
        }

        if (r1.isLoaded()) {
            Assert.fail("Loaded " + n + " traces, expected more");
        }
        if (r2.isLoaded()) {
            Assert.fail("Loaded excepted " + n + " traces, but more is available");
        }
    }

    private void assertSameState(StackTraceReader r1, StackTraceReader r2) {
        Assert.assertEquals("ThreadID", r1.getThreadId(), r2.getThreadId());
        Assert.assertEquals("ThreadName", r1.getThreadName(), r2.getThreadName());
        Assert.assertEquals("Timestamp", r1.getTimestamp(), r2.getTimestamp());
        Assert.assertEquals("State", r1.getThreadState(), r2.getThreadState());
        for(int i = 0; i != 32; ++i) {
            Assert.assertEquals("Counter #" + i, r1.getCounter(i), r2.getCounter(i));
        }
        assertEqualTraces(r1.getStackTrace(), r2.getStackTrace());
    }

    private void copyAllTraces(StackTraceReader reader, StackTraceWriter writer) throws IOException {
        if (!reader.isLoaded()) {
            reader.loadNext();
        }
        while(reader.isLoaded()) {
            copyTrace(reader, writer);
            reader.loadNext();
        }
    }

    private void copyTrace(StackTraceReader reader, StackTraceWriter writer) throws IOException {
        ThreadShapshot snap = new ThreadShapshot();
        snap.threadId = reader.getThreadId();
        snap.threadName = reader.getThreadName();
        snap.state = reader.getThreadState();
        snap.timestamp = reader.getTimestamp();
        for(int i = 0; i != 32; ++i) {
            snap.counters[i] = reader.getCounter(i);
        }
        snap.elements = reader.getTrace();
        writer.write(snap);
    }

    private void assertEqualTraces(StackFrame[] s1, StackFrame[] s2) {
        StringBuilder sb1 = new StringBuilder();
        for(StackFrame f: s1) {
            sb1.append(f.toString());
            sb1.append('\n');
        }
        StringBuilder sb2 = new StringBuilder();
        for(StackFrame f: s2) {
            sb2.append(f.toString());
            sb2.append('\n');
        }
        Assert.assertEquals("Stack trace", sb1.toString(), sb2.toString());
    }
}
