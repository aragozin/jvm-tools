package org.gridkit.jvmtool.stacktrace.analytics;

import java.io.IOException;
import java.lang.Thread.State;

import org.gridkit.jvmtool.stacktrace.CounterCollection;
import org.gridkit.jvmtool.stacktrace.ReaderProxy;
import org.gridkit.jvmtool.stacktrace.StackFrameList;
import org.gridkit.jvmtool.stacktrace.StackTraceReader;

public class FilteredStackTraceReader extends ReaderProxy implements StackTraceReader {

    private final ThreadSnapshotFilter filter;
    private final StackTraceReader reader;

    public FilteredStackTraceReader(ThreadSnapshotFilter filter, StackTraceReader reader) throws IOException {
        super(null);
        this.filter = filter;
        this.reader = reader;
        if (reader.isLoaded()) {
            reader.loadNext();
        }
        seek();
    }

    @Override
    protected StackTraceReader getReader() {
        return this;
    }

    @Override
    public boolean isLoaded() {
        return reader.isLoaded();
    }

    @Override
    public long getThreadId() {
        return reader.getThreadId();
    }

    @Override
    public long getTimestamp() {
        return reader.getTimestamp();
    }

    @Override
    public String getThreadName() {
        return reader.getThreadName();
    }

    @Override
    public State getThreadState() {
        return reader.getThreadState();
    }

    @Override
    public CounterCollection getCounters() {
        return reader.getCounters();
    }

    @Override
    public StackTraceElement[] getTrace() {
        return reader.getTrace();
    }

    @Override
    public StackFrameList getStackTrace() {
        return reader.getStackTrace();
    }

    private void seek() throws IOException {
        while(reader.isLoaded()) {
            if (filter.evaluate(this)) {
                return;
            }
            reader.loadNext();
        }
    }

    @Override
    public boolean loadNext() throws IOException {
        reader.loadNext();
        seek();
        return reader.isLoaded();
    }
}
