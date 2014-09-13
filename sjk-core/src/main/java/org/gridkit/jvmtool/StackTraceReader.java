package org.gridkit.jvmtool;

import java.io.IOException;

/**
 * @author Alexey Ragozin (alexey.ragozin@gmail.com)
 */
public interface StackTraceReader {

    public boolean isLoaded();

    public long getThreadId();

    public long getTimestamp();

    public StackTraceElement[] getTrace();

    public boolean loadNext() throws IOException;

}