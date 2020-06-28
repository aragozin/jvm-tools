package org.gridkit.jvmtool.spi.parsers;

public interface JsonEventDumpParser {

    /**
     * May return <code>null</code> or throw an exception if source is not supported by parser.
     */
    public JsonEventSource open(InputStreamSource source) throws Exception;

}
