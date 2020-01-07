package org.gridkit.jvmtool.stacktrace;

import java.io.IOException;
import java.lang.Thread.State;

/**
 * @author Alexey Ragozin (alexey.ragozin@gmail.com)
 */
public interface StackTraceReader {

    public boolean isLoaded();

    /**
     * File may include extra event besides thread dumps.
     * E.g. memory dynamics or OS metrics could be included
     * for further analysis.
     * <p>
     * Extra events are seconds class citizens so some readers
     * may filter them out.
     */
//    public String getEventType();

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

        @Override
        public boolean isLoaded() {
            return getReader().isLoaded();
        }

        @Override
        public long getThreadId() {
            return getReader().getThreadId();
        }

        @Override
        public long getTimestamp() {
            return getReader().getTimestamp();
        }

        @Override
        public String getThreadName() {
            return getReader().getThreadName();
        }

        @Override
        public State getThreadState() {
            return getReader().getThreadState();
        }

        @Override
        public CounterCollection getCounters() {
            return getReader().getCounters();
        }

        @Override
        public StackTraceElement[] getTrace() {
            return getReader().getTrace();
        }

        @Override
        public StackFrameList getStackTrace() {
            return getReader().getStackTrace();
        }

        @Override
        public boolean loadNext() throws IOException {
            return getReader().loadNext();
        }
    }
}
