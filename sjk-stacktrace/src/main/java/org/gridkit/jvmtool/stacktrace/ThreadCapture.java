package org.gridkit.jvmtool.stacktrace;

import java.lang.Thread.State;
import java.lang.management.ThreadInfo;

public class ThreadCapture {

    public long threadId;
    public String threadName;
    public long timestamp;
    public StackTraceElement[] elements;
    public CounterArray counters = new CounterArray();
    public State state;

    public void copyFrom(ThreadInfo info) {
        threadId = info.getThreadId();
        threadName = info.getThreadName();
        elements = info.getStackTrace();
        state = info.getThreadState();

        if (info.getBlockedCount() > 0) {
            counters.set(ThreadCounters.BLOCKED_COUNTER, info.getBlockedCount());
        }

        if (info.getBlockedTime() > 0) {
            counters.set(ThreadCounters.BLOCKED_TIME_MS, info.getBlockedTime());
        }

        if (info.getWaitedCount() > 0) {
            counters.set(ThreadCounters.WAIT_COUNTER, info.getWaitedCount());
        }

        if (info.getWaitedTime() > 0) {
            counters.set(ThreadCounters.WAIT_TIME_MS, info.getWaitedTime());
        }
    }

    public void reset() {
        threadId = -1;
        threadName = null;
        timestamp = -1;
        elements = null;
        counters.reset();
        state = null;
    }
}
