package org.gridkit.jvmtool.stacktrace.codec.json;

import java.io.IOException;

import org.gridkit.jvmtool.event.CommonEvent;
import org.gridkit.jvmtool.spi.parsers.JsonEventSource;

public interface JsonEventAdapter {

    /**
     * @return next available event at source or null if no events available
     */
    public CommonEvent parseNextEvent(JsonEventSource source) throws IOException;

}
