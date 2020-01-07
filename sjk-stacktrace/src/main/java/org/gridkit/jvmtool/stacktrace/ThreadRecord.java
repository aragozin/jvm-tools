package org.gridkit.jvmtool.stacktrace;

import java.lang.Thread.State;

import org.gridkit.jvmtool.event.SimpleTagCollection;
import org.gridkit.jvmtool.event.TagCollection;

public class ThreadRecord implements ThreadSnapshot {

    private long threadId = -1;
    private String threadName;
    private long timestamp = -1;
    private StackFrameList stackTrace;
    private State threadState;

    public ThreadRecord() {
    }

    public ThreadRecord(StackFrameList trace) {
        stackTrace = trace;
    }

    public ThreadRecord(StackFrameList trace, State state) {
        stackTrace = trace;
        threadState = state;
    }

    public void reset() {
        threadId = -1;
        threadName = null;
        timestamp = -1;
        stackTrace = null;
        threadState = null;
    }

    public void load(StackTraceReader reader) {
        reset();
        threadId = reader.getThreadId();
        threadName = reader.getThreadName();
        stackTrace = reader.getStackTrace();
        threadState = reader.getThreadState();
    }

    @Override
    public long threadId() {
        return threadId;
    }

    @Override
    public String threadName() {
        return threadName;
    }

    @Override
    public long timestamp() {
        return timestamp;
    }

    @Override
    public StackFrameList stackTrace() {
        return stackTrace;
    }

    @Override
    public State threadState() {
        return threadState;
    }

    @Override
    public CounterCollection counters() {
        return CounterArray.EMPTY;
    }

    @Override
    public TagCollection tags() {
        return SimpleTagCollection.EMPTY;
    }
}
