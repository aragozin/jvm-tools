package org.gridkit.jvmtool.codec.stacktrace;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.gridkit.jvmtool.codec.stacktrace.EventSeqEqualToCondition.exactlyAs;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import org.gridkit.jvmtool.event.ErrorEvent;
import org.gridkit.jvmtool.event.Event;
import org.gridkit.jvmtool.event.EventReader;
import org.gridkit.jvmtool.event.UniversalEventWriter;
import org.gridkit.jvmtool.stacktrace.CounterCollection;
import org.gridkit.jvmtool.stacktrace.StackFrame;
import org.gridkit.jvmtool.stacktrace.StackFrameList;
import org.gridkit.jvmtool.stacktrace.StackTraceReader;
import org.gridkit.jvmtool.stacktrace.ThreadEventCodec;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;

public class ThreadEventReaderTest {

    @Rule
    public TestName testName = new TestName();

    @Test
    public void read_dump_v1() throws FileNotFoundException, IOException {

        EventReader<Event> reader = ThreadEventCodec.createEventReader(new FileInputStream("src/test/resources/dump_v1.std"));

        int n = 0;
        for(Event e: reader) {
            if (e instanceof ErrorEvent) {
                fail("Error", ((ErrorEvent) e).exception());
            }
            ++n;
        }

        System.out.println("Read " + n + " traces from file");
    }

    @Test
    public void read_dump_v2() throws FileNotFoundException, IOException {

        EventReader<Event> reader = ThreadEventCodec.createEventReader(new FileInputStream("src/test/resources/dump_v2.std"));

        int n = 0;
        for(Event e: reader) {
            if (e instanceof ErrorEvent) {
                fail("Error", ((ErrorEvent) e).exception());
            }
//            System.out.println(((MultiCounterEvent)e).counters());
            ++n;
        }

        System.out.println("Read " + n + " traces from file");
    }

    @Test
    public void read_dump_v1_rewrite_and_compare() throws FileNotFoundException, IOException {

        String sourceFile = "src/test/resources/dump_v1.std";
        File file = new File("target/tmp/" + testName.getMethodName() + "-" + System.currentTimeMillis() + ".std");
        file.getParentFile().mkdirs();
        file.delete();

        FileOutputStream fow = new FileOutputStream(file);
        UniversalEventWriter writer = ThreadEventCodec.createEventWriter(fow);

        EventReader<Event> reader = ThreadEventCodec.createEventReader(new FileInputStream(sourceFile));

        copyAllTraces(reader, writer);
        writer.close();
        System.out.println("New file " + file.length() + " bytes (original " + new File(sourceFile).length() + " bytes)");

        reader = ThreadEventCodec.createEventReader(new FileInputStream(file));
        EventReader<Event> origReader = ThreadEventCodec.createEventReader(new FileInputStream("src/test/resources/dump_v1.std"));

        assertThat((Iterable<Event>)reader).is(exactlyAs(origReader));
    }

    @Test
    public void read_dump_v2_rewrite_and_compare() throws FileNotFoundException, IOException {

        File sourceFile = new File("src/test/resources/dump_v2.std");
        File file = new File("target/tmp/" + testName.getMethodName() + "-" + System.currentTimeMillis() + ".std");
        file.getParentFile().mkdirs();
        file.delete();

        FileOutputStream fow = new FileOutputStream(file);
        UniversalEventWriter writer = ThreadEventCodec.createEventWriter(fow);

        EventReader<Event> reader = ThreadEventCodec.createEventReader(new FileInputStream(sourceFile));

        copyAllTraces(reader, writer);
        writer.close();
        System.out.println("New file " + file.length() + " bytes (original " + sourceFile.length() + " bytes)");

        reader = ThreadEventCodec.createEventReader(new FileInputStream(file));
        EventReader<Event> origReader = ThreadEventCodec.createEventReader(new FileInputStream(sourceFile));

        assertThat((Iterable<Event>)reader).is(exactlyAs(origReader));
    }

    @Test
    public void read_dump_v4_rewrite_and_compare() throws FileNotFoundException, IOException {

        File sourceFile = new File("src/test/resources/dump_v4.std");
        File file = new File("target/tmp/" + testName.getMethodName() + "-" + System.currentTimeMillis() + ".std");
        file.getParentFile().mkdirs();
        file.delete();

        FileOutputStream fow = new FileOutputStream(file);
        UniversalEventWriter writer = ThreadEventCodec.createEventWriter(fow);

        EventReader<Event> reader = ThreadEventCodec.createEventReader(new FileInputStream(sourceFile));

        copyAllTraces(reader, writer);
        writer.close();
        System.out.println("New file " + file.length() + " bytes (original " + sourceFile.length() + " bytes)");

        reader = ThreadEventCodec.createEventReader(new FileInputStream(file));
        EventReader<Event> origReader = ThreadEventCodec.createEventReader(new FileInputStream(sourceFile));

        assertThat((Iterable<Event>)reader).is(exactlyAs(origReader));
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
        CounterCollection c1 = r1.getCounters();
        CounterCollection c2 = r2.getCounters();
        for(String key: c1) {
            Assert.assertEquals("Counter #" + key, c1.getValue(key), c2.getValue(key));
        }
        for(String key: c2) {
            Assert.assertEquals("Counter #" + key, c1.getValue(key), c2.getValue(key));
        }
        assertEqualTraces(r1.getStackTrace(), r2.getStackTrace());
    }

    private void copyAllTraces(EventReader<Event> reader, UniversalEventWriter writer) throws IOException {
        int tn = 0;
        int ntn = 0;
        for(Event e: reader) {
            if (e instanceof ThreadSnapshotEvent) {
                ++tn;
            }
            else {
                ++ntn;
            }
            writer.store(e);
        }
        System.out.println("Copy " + (tn + ntn) + " traces (" + tn + " + " + ntn + ")");
    }

    void assertEqualTraces(StackFrameList s1, StackFrameList s2) {
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
