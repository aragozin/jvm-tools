package org.gridkit.jvmtool.event;

import java.io.IOException;

public interface UniversalEventWriter {

    public void store(Event event) throws IOException;

    public void close() throws IOException;
}
