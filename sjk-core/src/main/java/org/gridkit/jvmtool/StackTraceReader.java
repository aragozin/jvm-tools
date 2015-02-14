package org.gridkit.jvmtool;

import java.io.IOException;
import java.lang.Thread.State;

import org.gridkit.jvmtool.stacktrace.StackFrame;
import org.gridkit.jvmtool.stacktrace.ThreadCounter;

/**
 * @author Alexey Ragozin (alexey.ragozin@gmail.com)
 */
public interface StackTraceReader {

    public boolean isLoaded();

    public long getThreadId();

    public long getTimestamp();

    public String getThreadName();

    public State getThreadState();

    public long getCounter(ThreadCounter counter);

    public long getCounter(int counterId);

    public StackTraceElement[] getTrace();

    public StackFrame[] getStackTrace();

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

        public long getCounter(ThreadCounter counter) {
            return getReader().getCounter(counter);
        }

        public long getCounter(int counterId) {
            return getReader().getCounter(counterId);
        }

        public StackTraceElement[] getTrace() {
            return getReader().getTrace();
        }

        public StackFrame[] getStackTrace() {
            return getReader().getStackTrace();
        }

        public boolean loadNext() throws IOException {
            return getReader().loadNext();
        }
    }
}