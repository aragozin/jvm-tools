package org.gridkit.jvmtool.stacktrace;

public interface CounterCollection extends Iterable<String> {

    public long getValue(String key);

    public CounterCollection clone();
}
