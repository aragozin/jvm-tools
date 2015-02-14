package org.gridkit.jvmtool;

import java.lang.Thread.State;
import java.util.Arrays;

public class ThreadShap {

    public long threadId;
    public String threadName;
    public long timestamp;
    public StackTraceElement[] elements;
    public long[] counters = new long[64];
    public State state;

    public void reset() {
        threadId = -1;
        threadName = null;
        timestamp = -1;
        elements = null;
        Arrays.fill(counters, -1);
        state = null;
    }
}
