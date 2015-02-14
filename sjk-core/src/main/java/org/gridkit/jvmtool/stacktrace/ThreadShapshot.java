package org.gridkit.jvmtool.stacktrace;

import java.lang.Thread.State;
import java.util.Arrays;

public class ThreadShapshot {

    public long threadId;
    public String threadName;
    public long timestamp;
    public StackTraceElement[] elements;
    public long[] counters = new long[32];
    public State state;

    public void setCounter(ThreadCounter c, long value) {
        counters[c.ordinal()] = value;
    }

    public void setCounter(int counterId, long value) {
        counters[counterId] = value;
    }

    public void reset() {
        threadId = -1;
        threadName = null;
        timestamp = -1;
        elements = null;
        Arrays.fill(counters, -1);
        state = null;
    }
}
