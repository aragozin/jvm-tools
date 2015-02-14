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

}