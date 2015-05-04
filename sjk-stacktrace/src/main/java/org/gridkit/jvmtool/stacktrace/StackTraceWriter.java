package org.gridkit.jvmtool.stacktrace;

import java.io.IOException;

public interface StackTraceWriter {

    /**
     * Appends another thread snapshot to stream.
     * @throws IOException
     */
    public void write(ThreadSnapshot snap) throws IOException;

    /**
     * Closes writer and flushes all underlying streams.
     */
    public void close();

}
