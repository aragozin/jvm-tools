package org.gridkit.jvmtool.stacktrace;

import java.io.IOException;
import java.lang.Thread.State;

/**
 * @author Alexey Ragozin (alexey.ragozin@gmail.com)
 */
public interface StackTraceReader {

    public boolean isLoaded();

    public long getThreadId();

    public long getTimestamp();

    public String getThreadName();

    public State getThreadState();

    public CounterCollection getCounters();

    public StackTraceElement[] getTrace();

    public StackFrameList getStackTrace();

    public boolean loadNext() throws IOException;

    public static abstract class StackTraceReaderDelegate implements StackTraceReader {

        protected abstract StackTraceReader getReader();

        public boolean isLoaded() {
            return getReader().isLoaded();
        }

        public long getThreadId() {
            return getReader().getThreadId();
        }

        public long getTimestamp() {
            return getReader().getTimestamp();
        }

        public String getThreadName() {
            return getReader().getThreadName();
        }

        public State getThreadState() {
            return getReader().getThreadState();
        }

        @Override
        public CounterCollection getCounters() {
            return getReader().getCounters();
        }

        public StackTraceElement[] getTrace() {
            return getReader().getTrace();
        }

        public StackFrameList getStackTrace() {
            return getReader().getStackTrace();
        }

        public boolean loadNext() throws IOException {
            return getReader().loadNext();
        }
    }
}