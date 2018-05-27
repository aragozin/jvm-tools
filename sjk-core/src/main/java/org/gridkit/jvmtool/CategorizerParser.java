/**
 * Copyright 2015 Alexey Ragozin
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
