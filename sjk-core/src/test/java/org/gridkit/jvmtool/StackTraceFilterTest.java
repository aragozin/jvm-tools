package org.gridkit.jvmtool;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.Test;

public class StackTraceFilterTest {

    @Test
    public void parse_seam_jsf_preset() throws FileNotFoundException {

        SectionParser sp = new SectionParser();
        Cascade.parse(new FileReader("src/test/resources/sample-seam-jsf-profile.shp"), sp);

    }


    class SectionParser {

        Map<String, StackFilterParser> sections = new LinkedHashMap<String, StackFilterParser>();

        @Cascade.Section
        public StackFilterParser section(String line) {
            line = line.trim();
            if (line.startsWith("[") && line.endsWith("]")) {
                String name = line.substring(1, line.length() - 1).trim();
                StackFilterParser node = StackFilterParser.anyNode();
                sections.put(name, node);
                return node;
            }
            else {
                throw new IllegalArgumentException("Expected section name: " + line);
            }
        }
    }
}
