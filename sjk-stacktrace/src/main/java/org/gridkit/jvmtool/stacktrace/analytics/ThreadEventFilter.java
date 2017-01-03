package org.gridkit.jvmtool.stacktrace.analytics;

import org.gridkit.jvmtool.codec.stacktrace.ThreadSnapshotEvent;
import org.gridkit.jvmtool.event.EventMorpher;

public class ThreadEventFilter implements EventMorpher<ThreadSnapshotEvent, ThreadSnapshotEvent> {

    private final ThreadSnapshotFilter filter;

    public ThreadEventFilter(ThreadSnapshotFilter filter) {
        this.filter = filter;
    }

    @Override
    public ThreadSnapshotEvent morph(ThreadSnapshotEvent event) {
        return filter.evaluate(event) ? event : null;
    }
}
