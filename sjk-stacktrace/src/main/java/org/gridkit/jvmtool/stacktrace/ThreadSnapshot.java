package org.gridkit.jvmtool.stacktrace;

import java.lang.Thread.State;

public interface ThreadSnapshot {

    public long threadId();

    /** may be <code>null</code> */
    public String threadName();
    
    public long timestamp();
    
    public StackFrameList stackTrace();
    
    /** may be <code>null</code> */
    public State threadState();
    
    public CounterCollection counters();
        
}
