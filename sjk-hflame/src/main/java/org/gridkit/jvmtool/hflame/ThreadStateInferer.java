package org.gridkit.jvmtool.hflame;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.gridkit.jvmtool.stacktrace.GenericStackElement;
import org.gridkit.jvmtool.stacktrace.ThreadSnapshot;
import org.gridkit.jvmtool.stacktrace.analytics.BasicFilterFactory;
import org.gridkit.jvmtool.stacktrace.analytics.ThreadSnapshotFilter;
import org.gridkit.jvmtool.stacktrace.analytics.TraceFilterPredicateParser;

public class ThreadStateInferer implements TraceMapper {

    public static final GenericTerminatingStackElement UNKNOWN = new GenericTerminatingStackElement("UNKNOWN");

    private static final List<String> PROPS = Arrays.asList("name", "rule", "prop");


    public static ThreadStateInferer loadFromResource(String source) {
        InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream(source);
        if (is == null) {
            throw new IllegalArgumentException("Unable to load resource: [source]");
        }
        Reader rdr = new InputStreamReader(is, Charset.forName("UTF8"));
        try {
            return new ThreadStateInferer(new BasicFilterFactory(), rdr);
        } catch (IOException e) {
            throw new IllegalArgumentException("Unable to parse resource: [source]", e);
        }
    }


    private List<Rule> rules = new ArrayList<Rule>();
    private Map<String, PseudoState> states = new HashMap<String, PseudoState>();

    public ThreadStateInferer(BasicFilterFactory filterFactory, Reader reader) throws IOException {
        new Loader(filterFactory).load(reader);
        for(PseudoState ps: states.values()) {
            if (ps.caption == null) {
                ps.caption = ps.id;
            }
            ps.element = new GenericTerminatingStackElement(ps.caption, ps.props);
        }
    }

    @Override
    public GenericStackElement generateTraceTerminator(ThreadSnapshot snap) {
        for(Rule rule: rules) {
            if (rule.filter.evaluate(snap)) {
                return rule.state.element;
            }
        }
        return UNKNOWN;
    }


    @SuppressWarnings("serial")
    private class Loader extends Properties {

        private final BasicFilterFactory filterFactory;

        public Loader(BasicFilterFactory filterFactory) {
            this.filterFactory = filterFactory;
        }

        @Override
        public synchronized Object put(Object key, Object value) {
            String k = (String) key;
            String v = (String) value;

            String[] split = k.split("[.]");
            if (split.length < 2) {
                throw invalidStatement(k, v);
            }
            String id = split[0];
            String prop = split[1];
            if (!PROPS.contains(prop)) {
                throw invalidStatement(k, v);
            }

            PseudoState state = states.get(id);
            if (state == null) {
                states.put(id, state = new PseudoState(id));
            }

            if (prop.equals("name")) {
                if (split.length > 2) {
                    throw invalidStatement(k, v);
                }
                state.caption = v;
            }
            else if (prop.equals("prop")) {
                if (split.length < 3) {
                    throw invalidStatement(k, v);
                }
                StringBuilder sb = new StringBuilder();
                for(int i = 2; i < split.length; ++i) {
                    if (sb.length() > 0) {
                        sb.append('.');
                    }
                    sb.append(split[i]);
                }
                state.props.put(sb.toString(), v);
            }
            else if (prop.equals("rule")) {
                ThreadSnapshotFilter filter = TraceFilterPredicateParser.parseFilter(v, filterFactory);
                Rule rule = new Rule();
                rule.filter = filter;
                rule.state = state;
                rules.add(rule);
            }

            return super.put(key, value);
        }

        private IllegalArgumentException invalidStatement(String k, String v) {
            throw new IllegalArgumentException("Invalid statement - " + k + ": " + v);
        }
    }

    private static class Rule {

        ThreadSnapshotFilter filter;
        PseudoState state;
    }

    private static class PseudoState {

        String id;
        String caption;
        GenericTerminatingStackElement element;
        Map<String, String> props = new HashMap<String, String>();

        public PseudoState(String id) {
            this.id = id;
        }
    }
}
