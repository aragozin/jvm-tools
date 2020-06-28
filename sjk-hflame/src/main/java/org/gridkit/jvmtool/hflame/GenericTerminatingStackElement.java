package org.gridkit.jvmtool.hflame;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import org.gridkit.jvmtool.stacktrace.GenericStackElement;

public class GenericTerminatingStackElement implements GenericStackElement {

    private final String caption;
    private final Map<String, String> props;

    public GenericTerminatingStackElement(String caption) {
        this.caption = caption;
        this.props = Collections.emptyMap();
    }

    public GenericTerminatingStackElement(String caption, Map<String, String> props) {
        this.caption = caption;
        this.props = Collections.unmodifiableMap(new LinkedHashMap<String, String>(props));
    }

    public Map<String, String> props() {
        return props;
    }

    @Override
    public String toString() {
        return caption;
    }
}
