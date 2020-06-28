package org.gridkit.jvmtool.spi.parsers;

import java.util.Map;

public interface JsonEventDumpParserFactory {

    public static final String OPT_USE_NATIVE_JFR_PARSER = "jfr.native_parser";
    public static final String OPT_JFR_EVENT_WHITELIST = "jfr.whitelist";
    public static final String OPT_JFR_EVENT_BLACKLIST = "jfr.blacklist";
    public static final String OPT_JSON_MAX_DEPTH = "json.max_depth";

    /**
     * Instantiates an instance of parser.
     * May fail or return <code>null</code>, that means parser is not available or is not compliant with options passed.
     */
    public JsonEventDumpParser createParser(Map<String, String> options) throws Exception;

}
