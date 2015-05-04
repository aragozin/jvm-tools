package org.gridkit.jvmtool.stacktrace;

import java.lang.Thread.State;

public class ReaderProxy implements ThreadSnapshot {
    
    protected StackTraceReader reader;

    public ReaderProxy(StackTraceReader reader) {
        super();
        this.reader = reader;
    }

    @Override
    public long threadId() {
        return reader.getThreadId();
    }

    @Override
    public String threadName() {
        return reader.getThreadName();
    }

    @Override
    public long timestamp() {
        return reader.getTimestamp();
    }

    @Override
    public StackFrameList stackTrace() {
        return reader.getStackTrace();
    }

    @Override
    public State threadState() {
        return reader.getThreadState();
    }

    @Override
    public CounterCollection counters() {
        return reader.getCounters();
    }
}
