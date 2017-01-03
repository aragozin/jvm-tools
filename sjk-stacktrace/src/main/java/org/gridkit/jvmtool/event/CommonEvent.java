package org.gridkit.jvmtool.event;

import org.gridkit.jvmtool.stacktrace.CounterCollection;

public interface CommonEvent extends TimestampedEvent, TaggedEvent, MultiCounterEvent {

    @Override
    public long timestamp();

    @Override
    public CounterCollection counters();

    @Override
    public TagCollection tags();

}
