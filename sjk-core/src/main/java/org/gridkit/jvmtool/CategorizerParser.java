package org.gridkit.jvmtool;

import java.io.IOException;
import java.io.Reader;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;

import org.gridkit.jvmtool.stacktrace.analytics.BasicFilterFactory;
import org.gridkit.jvmtool.stacktrace.analytics.SimpleCategorizer;
import org.gridkit.jvmtool.stacktrace.analytics.ThreadSnapshotFilter;
import org.gridkit.jvmtool.stacktrace.analytics.TraceFilterPredicateParser;

public class CategorizerParser {

    public static void loadCategories(Reader source, SimpleCategorizer categorizer, boolean shortNames, final BasicFilterFactory factory) throws IOException {
        
        final Map<String, ThreadSnapshotFilter> filters = new LinkedHashMap<String, ThreadSnapshotFilter>();
        
        @SuppressWarnings("serial")
        Properties props = new Properties() {

            @Override
            public synchronized Object put(Object key, Object value) {
                String skey = (String) key;
                String svalue = (String) value;
                if (!skey.endsWith("._")) {
                    ThreadSnapshotFilter filter = TraceFilterPredicateParser.parseFilter(svalue, factory);
                    filters.put(skey, filter);
                }      
                return super.put(key, value);
            }            
        };
        
        props.load(source);    
        
        for(String cat: filters.keySet()) {
            String name = cat;
            if (!shortNames) {
                String desc = props.getProperty(name + "._");
                if (desc != null) {
                    name = desc;
                }
            }
            categorizer.addCategory(name, filters.get(cat));
        }
    }    
}
