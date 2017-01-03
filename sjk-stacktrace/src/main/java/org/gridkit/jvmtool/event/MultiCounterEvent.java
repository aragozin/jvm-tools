package org.gridkit.jvmtool.event;

import org.gridkit.jvmtool.stacktrace.CounterCollection;

public interface MultiCounterEvent extends Event {

    public CounterCollection counters();

}
