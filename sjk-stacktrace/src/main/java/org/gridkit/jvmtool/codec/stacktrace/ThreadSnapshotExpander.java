package org.gridkit.jvmtool.codec.stacktrace;

import org.gridkit.jvmtool.event.Event;
import org.gridkit.jvmtool.event.EventMorpher;

public class ThreadSnapshotExpander implements EventMorpher<Event, Event> {

    private ThreadSnapshotEventPojo pojo = new ThreadSnapshotEventPojo();

    @Override
    public Event morph(Event event) {
        if (event instanceof ThreadTraceEvent && !(event instanceof ThreadSnapshotEvent)) {
            pojo.loadFromRawEvent((ThreadTraceEvent) event);
            return pojo;
        }
        else {
            return event;
        }
    }
}
