package org.gridkit.jvmtool.stacktrace;

import java.lang.Thread.State;
import java.lang.management.ThreadInfo;
import java.util.Arrays;
import java.util.Iterator;

import org.gridkit.jvmtool.event.SimpleTagCollection;
import org.gridkit.jvmtool.event.TagCollection;

public class ThreadCapture implements ThreadSnapshot {

    public long threadId;
    public String threadName;
    public long timestamp;
    public StackTraceElement[] elements;
    public CounterArray counters = new CounterArray();
    public TagCollection tags = new SimpleTagCollection();
    public State state;

    @Override
    public long threadId() {
        return threadId;
    }

    @Override
    public String threadName() {
        return threadName;
    }

    @Override
    public long timestamp() {
        return timestamp;
    }

    @Override
    public StackFrameList stackTrace() {
        return new StackProxy(elements);
    }

    @Override
    public State threadState() {
        return state;
    }

    @Override
    public CounterCollection counters() {
        return counters;
    }

    @Override
    public TagCollection tags() {
        return tags;
    }

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

    class StackProxy implements StackFrameList {

        StackTraceElement[] stack;

        public StackProxy(StackTraceElement[] stack) {
            this.stack = stack;
        }

        @Override
        public Iterator<StackFrame> iterator() {
            final Iterator<StackTraceElement> it = Arrays.asList(stack).iterator();
            return new Iterator<StackFrame>() {

                @Override
                public boolean hasNext() {
                    return it.hasNext();
                }

                @Override
                public StackFrame next() {
                    return new StackFrameWrapper(it.next());
                }

                @Override
                public void remove() {
                    it.remove();
                }
            };
        }

        @Override
        public StackFrame frameAt(int n) {
            return new StackFrameWrapper(stack[n]);
        }

        @Override
        public int depth() {
            return stack.length;
        }

        @Override
        public StackFrameList fragment(int from, int to) {
            throw new UnsupportedOperationException();
        }

        @Override
        public StackFrame[] toArray() {
            StackFrame[] frames = new StackFrame[stack.length];
            for(int i = 0; i !=  stack.length; ++i) {
                frames[i] = new StackFrameWrapper(stack[i]);
            }
            return frames;
        }

        @Override
        public boolean isEmpty() {
            return stack.length == 0;
        }
    }

    static class StackFrameWrapper extends StackFrame {

        StackTraceElement ste;

        public StackFrameWrapper(StackTraceElement ste) {
            super(ste);
            this.ste = ste;
        }

        @Override
        public StackTraceElement toStackTraceElement() {
            return ste;
        }
    }
}
