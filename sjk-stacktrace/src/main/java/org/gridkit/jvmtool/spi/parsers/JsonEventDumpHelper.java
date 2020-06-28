package org.gridkit.jvmtool.spi.parsers;

import java.io.IOException;
import java.util.Iterator;
import java.util.Map;
import java.util.ServiceLoader;

public class JsonEventDumpHelper {

    public static JsonEventSource open(InputStreamSource source, Map<String, String> options) throws IOException {
        StringBuilder sb = new StringBuilder();
        ServiceLoader<JsonEventDumpParserFactory> factories = ServiceLoader.load(JsonEventDumpParserFactory.class);
        Iterator<JsonEventDumpParserFactory> it = factories.iterator();
        while(it.hasNext()) {
            try {
                JsonEventDumpParserFactory factory = it.next();
                JsonEventDumpParser parser = factory.createParser(options);
                if (parser != null) {
                    try {
                        JsonEventSource src = parser.open(source);
                        if (src != null) {
                            return src;
                        }
                        sb.append(" " + parser.toString() + " -> unsupported format\n");
                    }
                    catch(Exception e) {
                        String pname = parser.toString();
                        sb.append(" " + pname + " -> " + e.getMessage() + "\n");
                    }
                }
            }
            catch(Throwable e) {
                // ignore
            }
        }
        // none of parser was
        if (sb.length() == 0) {
            throw new IOException("Unable to open dump, no parsers are available");
        }
        else {
            throw new IOException("Unable to open dump\n" + sb.toString());
        }
    }
}
