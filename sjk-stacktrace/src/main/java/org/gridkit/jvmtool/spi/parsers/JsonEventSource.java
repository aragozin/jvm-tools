package org.gridkit.jvmtool.spi.parsers;

import java.io.IOException;

import org.gridkit.jvmtool.util.json.JsonStreamWriter;

public interface JsonEventSource {

    /**
     * @return <code>false</code> if no more data
     */
    public boolean readNext(JsonStreamWriter writer) throws IOException;

}
