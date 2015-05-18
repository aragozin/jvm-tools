package org.gridkit.jvmtool.stacktrace;

import java.io.IOException;

public interface FlatSampleWriter {

    public FlatSampleWriter set(String field, long value);

    public FlatSampleWriter set(String field, double value);

    public FlatSampleWriter set(String field, String value);

    public void push() throws IOException;
    
    public void close() throws IOException;
}
