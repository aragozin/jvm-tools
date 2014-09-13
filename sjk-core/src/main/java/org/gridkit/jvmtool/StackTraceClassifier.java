package org.gridkit.jvmtool;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

import org.gridkit.jvmtool.StackTraceFilter.AnyMatcher;
import org.gridkit.jvmtool.StackTraceFilter.RequiredMatcher;

public class StackTraceClassifier {

    private StackTraceFilter root;
    private Map<String, StackTraceFilter> filters;

    public StackTraceClassifier(StackTraceFilter root, Map<String, StackTraceFilter> filters) {
        this.root = root;
        this.filters = filters;
    }

    public Collection<String> getClasses() {
        return filters.keySet();
    }

    public String classify(StackTraceElement[] trace) {
        if (root == null || root.evaluate(trace)) {
            for(String cl: filters.keySet()) {
                if (filters.get(cl).evaluate(trace)) {
                    return cl;
                }
            }
            return null;
        }
        else {
            return null;
        }
    }

    public static class Config {

        private Map<String, StackFilterParser> sections = new LinkedHashMap<String, StackFilterParser>();

        @Cascade.Section
        public StackFilterParser section(String line) {
            line = line.trim();
            if (line.startsWith("[") && line.endsWith("]")) {
                String name = line.substring(1, line.length() - 1).trim();
                if (sections.containsKey(name)) {
                    throw new IllegalArgumentException("Section [" + name + "] is already exists");
                }
                StackFilterParser node = StackFilterParser.anyNode();
                sections.put(name, node);
                return node;
            }
            else {
                throw new IllegalArgumentException("Expected section name: " + line);
            }
        }

        public StackTraceClassifier create() {
            StackFilterParser proot = sections.remove("ROOT");
            StackTraceFilter root = proot == null ? null : proot.getFilter();

            Map<String, StackTraceFilter> fmap = new LinkedHashMap<String, StackTraceFilter>();
            for(String cl: sections.keySet()) {
                StackTraceFilter f = sections.get(cl).getFilter();
                if (f instanceof AnyMatcher) {
                    if (((AnyMatcher) f).alternatives.length == 0) {
                        f = new RequiredMatcher(new StackTraceFilter[0]);
                    }
                }
                fmap.put(cl, f);
            }

            return new StackTraceClassifier(root, fmap);
        }
    }
}
