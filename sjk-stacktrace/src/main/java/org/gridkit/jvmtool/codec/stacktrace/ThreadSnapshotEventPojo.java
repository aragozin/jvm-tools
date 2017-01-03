package org.gridkit.jvmtool.codec.stacktrace;

import java.lang.Thread.State;

import org.gridkit.jvmtool.event.CommonEvent;
import org.gridkit.jvmtool.event.GenericEvent;
import org.gridkit.jvmtool.event.TaggedEvent;
import org.gridkit.jvmtool.jvmevents.JvmEvents;
import org.gridkit.jvmtool.stacktrace.StackFrameList;
import org.gridkit.jvmtool.stacktrace.ThreadSnapshot;

public class ThreadSnapshotEventPojo extends GenericEvent implements ThreadSnapshotEvent {

    private long threadId = -1;
    private String threadName;
    private State threadState;
    private StackFrameList stackTrace;

    @Override
    public long threadId() {
        return threadId;
    }

    public void threadId(long threadId) {
        this.threadId = threadId;
    }

    @Override
    public String threadName() {
        return threadName;
    }

    public void threadName(String name) {
        this.threadName = name;
    }

    @Override
    public State threadState() {
        return threadState;
    }

    public void threadState(State state) {
        this.threadState = state;
    }

    @Override
    public StackFrameList stackTrace() {
        return stackTrace;
    }

    public void stackTrace(StackFrameList trace) {
        this.stackTrace = trace;
    }

    public void loadFrom(ThreadSnapshot event) {
        if (event instanceof ThreadSnapshotEvent) {
            loadFrom((ThreadSnapshotEvent)event);
        }
        else {
            timestamp(event.timestamp());
            threadId(event.threadId());
            threadName(event.threadName());
            threadState(event.threadState());
            counters().clear();;
            counters().setAll(event.counters());
            stackTrace(event.stackTrace());
            if (event instanceof TaggedEvent) {
                tags().clear();
                tags().putAll(((TaggedEvent) event).tags());
            }
        }
    }

    public void loadFrom(ThreadSnapshotEvent event) {
        copyCommonEventFrom(event);
        threadId(event.threadId());
        threadName(event.threadName());
        threadState(event.threadState());
        stackTrace(event.stackTrace());
    }

    public void loadFromRawEvent(ThreadTraceEvent event) {
        threadId(-1);
        threadName(null);
        threadState(null);
        CommonEvent cevent = (CommonEvent)event;
        copyCommonEventFrom(cevent);
        if (cevent.counters().getValue(JvmEvents.THREAD_ID) >= 0) {
            threadId(cevent.counters().getValue(JvmEvents.THREAD_ID));
        }
        threadName(cevent.tags().firstTagFor(JvmEvents.THREAD_NAME));
        threadState(state(cevent.tags().firstTagFor(JvmEvents.THREAD_STATE)));
        stackTrace(event.stackTrace());
    }

    private State state(String state) {
        try {
            return state == null ? null : Thread.State.valueOf(state);
        } catch (Exception e) {
            return null;
        }
    }
}
