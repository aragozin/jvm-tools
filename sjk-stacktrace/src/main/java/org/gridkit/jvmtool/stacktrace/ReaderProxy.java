package org.gridkit.jvmtool.stacktrace;

import java.lang.Thread.State;

import org.gridkit.jvmtool.event.SimpleTagCollection;
import org.gridkit.jvmtool.event.TagCollection;
import org.gridkit.jvmtool.stacktrace.StackTraceReader.StackTraceReaderDelegate;

public class ReaderProxy extends StackTraceReaderDelegate implements ThreadSnapshot {

    protected StackTraceReader reader;

    public ReaderProxy(StackTraceReader reader) {
        super();
        this.reader = reader;
    }

    @Override
    protected StackTraceReader getReader() {
        return reader;
    }

    @Override
    public long threadId() {
        return getThreadId();
    }

    @Override
    public String threadName() {
        return getThreadName();
    }

    @Override
    public long timestamp() {
        return getTimestamp();
    }

    @Override
    public StackFrameList stackTrace() {
        return getStackTrace();
    }

    @Override
    public State threadState() {
        return getThreadState();
    }

    @Override
    public CounterCollection counters() {
        return getCounters();
    }

    @Override
    public TagCollection tags() {
        return SimpleTagCollection.EMPTY;
    }
}
