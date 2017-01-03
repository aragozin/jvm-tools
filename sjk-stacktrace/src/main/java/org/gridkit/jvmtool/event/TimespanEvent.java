package org.gridkit.jvmtool.event;

public interface TimespanEvent extends TimestampedEvent {

    // by convention duration is expected to be in microseconds
    public long duration();

}
