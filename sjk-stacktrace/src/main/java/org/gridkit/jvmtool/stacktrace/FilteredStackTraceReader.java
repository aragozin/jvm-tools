package org.gridkit.jvmtool.stacktrace;

import java.io.IOException;

import org.gridkit.jvmtool.stacktrace.StackTraceReader.StackTraceReaderDelegate;

public abstract class FilteredStackTraceReader extends StackTraceReaderDelegate {

    private StackTraceReader delegate;

    public FilteredStackTraceReader(StackTraceReader delegate) {
        super();
        this.delegate = delegate;
    }

    protected abstract boolean evaluate();

    @Override
    protected StackTraceReader getReader() {
        return delegate;
    }

    @Override
    public boolean loadNext() throws IOException {
        while(delegate.loadNext()) {
            if (evaluate()) {
                return true;
            }
        }
        return false;
    }
}
