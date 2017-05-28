package org.gridkit.jvmtool.stacktrace;

import java.io.IOException;
import java.lang.Thread.State;

import org.gridkit.jvmtool.codec.stacktrace.ThreadSnapshotEvent;
import org.gridkit.jvmtool.event.Event;
import org.gridkit.jvmtool.event.EventReader;
import org.gridkit.jvmtool.event.ShieldedEventReader;

public class LegacyStackReader implements StackTraceReader {

    private EventReader<ThreadSnapshotEvent> events;

    public LegacyStackReader(EventReader<? extends Event> source) {
        events = new ShieldedEventReader<ThreadSnapshotEvent>(source, ThreadSnapshotEvent.class);
    }

    @Override
    public boolean isLoaded() {
        return events.hasNext();
    }

    @Override
    public long getThreadId() {
        return events.peekNext().threadId();
    }

    @Override
    public long getTimestamp() {
        return events.peekNext().timestamp();
    }

    @Override
    public String getThreadName() {
        return events.peekNext().threadName();
    }

    @Override
    public State getThreadState() {
        return events.peekNext().threadState();
    }

    @Override
    public CounterCollection getCounters() {
        return events.peekNext().counters();
    }

    @Override
    public StackTraceElement[] getTrace() {
        StackFrameList list = events.peekNext().stackTrace();
        if (list == null) {
            return null;
        }
        else {
            StackTraceElement[] strace = new StackTraceElement[list.depth()];
            for(int i = 0; i != strace.length; ++i) {
                strace[i] = list.frameAt(i).toStackTraceElement();
            }
            return strace;
        }
    }

    @Override
    public StackFrameList getStackTrace() {
        return events.peekNext().stackTrace();
    }

    @Override
    public boolean loadNext() throws IOException {
        if (events.hasNext()) {
            events.next();
        }
        return events.hasNext();
    }
}
