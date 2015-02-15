package org.gridkit.jvmtool.stacktrace;

import java.lang.Thread.State;
import java.lang.management.ThreadInfo;
import java.util.Arrays;

public class ThreadSnapshot {

    public long threadId;
    public String threadName;
    public long timestamp;
    public StackTraceElement[] elements;
    public long[] counters = new long[32];
    public State state;

    public void copyFrom(ThreadInfo info) {
        threadId = info.getThreadId();
        threadName = info.getThreadName();
        elements = info.getStackTrace();
        state = info.getThreadState();

        if (info.getBlockedCount() > 0) {
            setCounter(ThreadCounter.BLOCKED_COUNTER, info.getBlockedCount());
        }

        if (info.getBlockedTime() > 0) {
            setCounter(ThreadCounter.BLOCKED_TIME, info.getBlockedTime());
        }

        if (info.getWaitedCount() > 0) {
            setCounter(ThreadCounter.WAIT_COUNTER, info.getWaitedCount());
        }

        if (info.getWaitedTime() > 0) {
            setCounter(ThreadCounter.WAIT_TIME, info.getWaitedTime());
        }
    }

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
