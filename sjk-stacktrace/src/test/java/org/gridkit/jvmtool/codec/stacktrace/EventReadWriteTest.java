package org.gridkit.jvmtool.codec.stacktrace;

import static org.assertj.core.api.Assertions.assertThat;
import static org.gridkit.jvmtool.codec.stacktrace.EventEqualToCondition.eventEquals;
import static org.gridkit.jvmtool.codec.stacktrace.EventSeqEqualToCondition.exactlyAs;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.Thread.State;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

import org.gridkit.jvmtool.event.CommonEvent;
import org.gridkit.jvmtool.event.ErrorEvent;
import org.gridkit.jvmtool.event.Event;
import org.gridkit.jvmtool.event.EventReader;
import org.gridkit.jvmtool.event.MorphingEventReader;
import org.gridkit.jvmtool.event.SimpleTagCollection;
import org.gridkit.jvmtool.event.TagCollection;
import org.gridkit.jvmtool.event.UniversalEventWriter;
import org.gridkit.jvmtool.stacktrace.CounterCollection;
import org.gridkit.jvmtool.stacktrace.StackFrame;
import org.gridkit.jvmtool.stacktrace.StackFrameArray;
import org.gridkit.jvmtool.stacktrace.StackFrameList;
import org.gridkit.jvmtool.stacktrace.ThreadEventCodec;
import org.junit.Test;

public class EventReadWriteTest {

    private EventReader<Event> writeReadEvents(Event... events) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();

        UniversalEventWriter writer = ThreadEventCodec.createEventWriter(bos);
        for(Event e: events) {
            writer.store(e);
        }
        writer.close();

        byte[] data = bos.toByteArray();
        System.out.println("Encoded " + events.length + " events into " + data.length + " bytes");

        ByteArrayInputStream bis = new ByteArrayInputStream(data);

        EventReader<Event> reader = ThreadEventCodec.createEventReader(bis);
        return reader;
    }

    private EventReader<Event> safeWriteReadEvents(Event... events) throws IOException {
        return new MorphingEventReader<Event>(writeReadEvents(events)) {

            @Override
            protected Event transform(Event event) {
                if (event instanceof ErrorEvent) {
                    throw new RuntimeException(((ErrorEvent) event).exception());
                }
                return event;
            }
        };
    }

    @Test
    public void verify_simple_event() throws Exception {

        TestDataEvent a = new TestDataEvent();

        a.timestamp(1000);
        a.tag("A", "1");
        a.tag("B", "1");
        a.tag("B", "2");
        a.tag("C", "0");
        a.set("qwerty", 10);
        a.set("QWERTY", Long.MAX_VALUE - Integer.MAX_VALUE);

        CommonEvent b = (CommonEvent) safeWriteReadEvents(a).iterator().next();

        assertThat(b).isNotInstanceOf(ThreadSnapshotEvent.class);
        assertThat(b.timestamp()).isEqualTo(1000);
        assertThat(b.counters().getValue("qwerty")).isEqualTo(10);
        assertThat(b.counters().getValue("QWERTY")).isEqualTo(Long.MAX_VALUE - Integer.MAX_VALUE);
        assertThat(b.tags()).containsOnly("A", "B", "C");
        assertThat(b.tags().tagsFor("A")).containsOnly("1");
        assertThat(b.tags().tagsFor("B")).containsOnly("1", "2");
        assertThat(b.tags().tagsFor("C")).containsOnly("0");

        assertThat(b).is(eventEquals(a));
    }


    @Test
    public void verify_3_same_events() throws Exception {

        TestDataEvent a = new TestDataEvent();

        a.timestamp(1000);
        a.tag("A", "1");
        a.tag("B", "1");
        a.tag("B", "2");
        a.tag("C", "0");
        a.set("qwerty", 10);
        a.set("QWERTY", Long.MAX_VALUE - Integer.MAX_VALUE);

        EventReader<Event> reader = safeWriteReadEvents(a, a, a);

        assertThat((Iterator<Event>)reader).is(EventSeqEqualToCondition.exactlyAs(a, a, a));
    }

    @Test
    public void verify_more_simple_events() throws Exception {

        TestDataEvent a1 = new TestDataEvent();

        a1.timestamp(1000);
        a1.tag("A", "1");
        a1.tag("B", "1");
        a1.tag("B", "2");
        a1.tag("C", "0");
        a1.set("qwerty", 10);
        a1.set("QWERTY", Long.MAX_VALUE - Integer.MAX_VALUE);

        TestDataEvent a2 = new TestDataEvent();

        a2.timestamp(2000);
        a2.tag("AA", "1");
        a2.tag("BB", "1");
        a2.tag("BB", "2");
        a2.tag("CC", "0");
        a2.set("Q", 20);
        a2.set("q", Long.MAX_VALUE - Integer.MAX_VALUE);

        EventReader<Event> reader = safeWriteReadEvents(a1, a2, a1, a2, a1);

        assertThat(reader.next()).is(eventEquals(a1));
        assertThat(reader.next()).is(eventEquals(a2));
        assertThat(reader.next()).is(eventEquals(a1));
        assertThat(reader.next()).is(eventEquals(a2));
        assertThat(reader.next()).is(eventEquals(a1));
        assertThat(reader.hasNext()).isFalse();
        assertThat(reader.hasNext()).isFalse();
    }

    @Test
    public void verify_thread_snapshot() throws IOException {

        ThreadEvent a = new ThreadEvent();
        a.timestamp(10000);
        a.threadId(100);
        a.threadName("Abc");
        a.stackTrace(genTrace(10));

        ThreadSnapshotEvent b = (ThreadSnapshotEvent) safeWriteReadEvents(a).iterator().next();

        assertThat(b.timestamp()).isEqualTo(10000);
        assertThat(b.threadId()).isEqualTo(100);
        assertThat(b.threadName()).isEqualTo("Abc");
        assertThat(b.threadState()).isNull();
        assertThat(b.counters()).containsExactly("thread.javaId");
        assertThat(b.tags().toString()).isEqualTo("[thread.javaName:Abc]");

        assertThat(b).is(eventEquals(a));
    }

    @Test
    public void verify_mixed_event_set() throws IOException {

        ThreadEvent a1 = new ThreadEvent();
        a1.timestamp(10000);
        a1.threadId(100);
        a1.threadName("Abc");
        a1.stackTrace(genTrace(10));

        ThreadEvent a2 = new ThreadEvent();
        a2.timestamp(20000);
        a2.threadId(101);
        a2.threadName("xYZ");
        a2.stackTrace(genTrace(5));
        a2.set("thread.cpu", 1000);

        TestDataEvent a3 = new TestDataEvent();

        a3.timestamp(1000);
        a3.tag("A", "1");
        a3.tag("B", "1");
        a3.tag("B", "2");
        a3.tag("C", "0");
        a3.set("qwerty", 10);
        a3.set("QWERTY", Long.MAX_VALUE - Integer.MAX_VALUE);

        TestDataEvent a4 = new TestDataEvent();

        a4.timestamp(2000);
        a4.tag("AA", "1");
        a4.tag("BB", "1");
        a4.tag("BB", "2");
        a4.tag("CC", "0");
        a4.set("Q", 20);
        a4.set("qwerty", Long.MAX_VALUE - Integer.MAX_VALUE);


        EventReader<Event> reader = safeWriteReadEvents(a1, a2, a3, a4 , a1, a3, a2, a4);

        assertThat((Iterable<Event>)reader).is(exactlyAs(a1, a2, a3, a4 , a1, a3, a2, a4));
    }

    private StackTraceElement[] genTrace(int n) {
        if (n == 0) {
            return Thread.currentThread().getStackTrace();
        }
        else if (n == 1) {
            return genTrace(n - 1);
        }
        else if (n == 2) {
            return genTrace(n - 1);
        }
        else if (n == 3) {
            return genTrace(n - 1);
        }
        else if (n == 4) {
            return genTrace(n - 1);
        }
        else if (n == 5) {
            return genTrace(n - 1);
        }
        else {
            return genTrace(n - 1);
        }
    }

    public static class TestDataEvent implements CommonEvent {

        long timestamp = 0;
        SimpleTagCollection tags = new SimpleTagCollection();
        TestCounterCollection counters = new TestCounterCollection();

        @Override
        public long timestamp() {
            return timestamp;
        }

        @Override
        public CounterCollection counters() {
            return counters;
        }

        @Override
        public TagCollection tags() {
            return tags;
        }

        public void timestamp(long timestamp) {
            this.timestamp = timestamp;
        }

        public void tag(String key, String tag) {
            tags.put(key, tag);
        }

        public void set(String key, long value) {
            counters.set(key, value);
        }
    }

    public static class ThreadEvent extends TestDataEvent implements ThreadSnapshotEvent {

        String threadName;
        long threadId = -1;
        State state = null;
        StackFrameList trace;

        @Override
        public long threadId() {
            return threadId;
        }

        public void threadId(long threadId) {
            this.threadId = threadId;
            counters.set("thread.javaId", threadId);
        }

        @Override
        public String threadName() {
            return threadName;
        }

        public void threadName(String threadName) {
            this.threadName = threadName;
            tags.put("thread.javaName", threadName);
        }

        @Override
        public State threadState() {
            return state;
        }

        public void threadState(State state) {
            this.state = state;
        }

        @Override
        public StackFrameList stackTrace() {
            return trace;
        }

        public void stackTrace(StackTraceElement[] trace) {
            StackFrame[] ftrace = new StackFrame[trace.length];
            for(int i = 0; i != trace.length; ++i) {
                ftrace[i] = new StackFrame(trace[i]);
            }
            this.trace = new StackFrameArray(ftrace);
        }
    }

    private static class TestCounterCollection implements CounterCollection {

        private Map<String, Long> counters = new TreeMap<String, Long>();

        public void set(String key, long value) {
            counters.put(key, value);
        }

        @Override
        public Iterator<String> iterator() {
            return counters.keySet().iterator();
        }

        @Override
        public long getValue(String key) {
            Long n  = counters.get(key);
            return  n == 0 ? Long.MIN_VALUE : n;
        }

        public TestCounterCollection clone() {
            throw new UnsupportedOperationException();
        }

        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append('[');
            for(String key: this) {
                if (sb.length() > 1) {
                    sb.append(", ");
                }
                sb.append(key).append(": ").append(getValue(key));
            }
            sb.append(']');
            return sb.toString();
        }
    }
}
