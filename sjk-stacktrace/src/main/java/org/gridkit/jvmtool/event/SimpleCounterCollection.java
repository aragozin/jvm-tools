package org.gridkit.jvmtool.event;

import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

import org.gridkit.jvmtool.stacktrace.CounterCollection;

public class SimpleCounterCollection implements CounterCollection {

    private Map<String, Long> counters = new TreeMap<String, Long>();

    @Override
    public Iterator<String> iterator() {
        return counters.keySet().iterator();
    }

    @Override
    public long getValue(String key) {
        Long n  = counters.get(key);
        return  n == null ? Long.MIN_VALUE : n;
    }

    public void set(String key, long value) {
        counters.put(key, value);
    }

    public void setAll(CounterCollection that) {
        for(String key: that) {
            set(key, that.getValue(key));
        }
    }

    public SimpleCounterCollection clone() {
        try {
            SimpleCounterCollection that = (SimpleCounterCollection) super.clone();
            that.counters = new TreeMap<String, Long>(counters);
            return that;
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException(e);
        }
    }

    public void clear() {
        counters.clear();
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append('[');
        for(String key: this) {
            if (sb.length() > 1) {
                sb.append(", ");
            }
            sb.append(key).append(": ").append(getValue(key));
        }
        sb.append(']');
        return sb.toString();
    }
}
