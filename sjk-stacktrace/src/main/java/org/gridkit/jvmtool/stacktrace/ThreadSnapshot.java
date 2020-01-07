package org.gridkit.jvmtool.stacktrace;

import java.lang.Thread.State;

import org.gridkit.jvmtool.event.CommonEvent;

public interface ThreadSnapshot extends CommonEvent {

    public long threadId();

    /** may be <code>null</code> */
    public String threadName();

    @Override
    public long timestamp();

    /** may be <code>null</code> */
    public State threadState();

    public StackFrameList stackTrace();

    @Override
    public CounterCollection counters();

}
