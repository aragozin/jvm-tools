package org.gridkit.jvmtool.stacktrace;

import java.io.IOException;
import java.lang.Thread.State;
import java.util.Iterator;
import java.util.NoSuchElementException;

import org.gridkit.jvmtool.codec.stacktrace.ThreadSnapshotEvent;
import org.gridkit.jvmtool.event.ErrorEvent;
import org.gridkit.jvmtool.event.Event;
import org.gridkit.jvmtool.event.EventMorpher;
import org.gridkit.jvmtool.event.EventReader;
import org.gridkit.jvmtool.event.MorphingEventReader;
import org.gridkit.jvmtool.event.SimpleCounterCollection;
import org.gridkit.jvmtool.event.SimpleErrorEvent;
import org.gridkit.jvmtool.event.SimpleTagCollection;
import org.gridkit.jvmtool.event.TagCollection;
import org.gridkit.jvmtool.jvmevents.JvmEvents;

public class LegacyThreadEventReader implements EventReader<Event> {

    private final StackTraceReader reader;
    private final Proxy proxy = new Proxy();
    private ErrorEvent error;
    private boolean goNext;

    public LegacyThreadEventReader(StackTraceReader reader) {
        this.reader = reader;
        if (reader.isLoaded()) {
            proxy.init();
        }
    }

    @Override
    public <M extends Event> EventReader<M> morph(EventMorpher<Event, M> morpher) {
        return MorphingEventReader.morph(this, morpher);
    }

    @Override
    public boolean hasNext() {

        if (error != null) {
            return !goNext;
        }

        try {
            if (goNext || !reader.isLoaded()) {
                goNext = false;
                if (reader.loadNext()) {
                    proxy.init();
                }
            }
            return reader.isLoaded();
        } catch (IOException e) {
            error = new SimpleErrorEvent(e);
            return true;
        }
    }

    @Override
    public Event next() {
        if (!hasNext()) {
            throw new NoSuchElementException();
        }
        goNext = true;
        return error == null ? proxy : error;
    }

    @Override
    public Event peekNext() {
        if (!hasNext()) {
            throw new NoSuchElementException();
        }
        return error == null ? proxy : error;
    }

    @Override
    public void remove() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Iterator<Event> iterator() {
        return this;
    }

    @Override
    public void dispose() {
        // do nothing
    }

    private class Proxy implements ThreadSnapshotEvent {

        private SimpleCounterCollection counters = new SimpleCounterCollection();
        private SimpleTagCollection tags = new SimpleTagCollection();

        public void init() {
            counters.clear();
            counters.setAll(reader.getCounters());
            if (reader.getThreadId() >= 0) {
                counters.set(JvmEvents.THREAD_ID, reader.getThreadId());
            }
            tags.clear();
            if (reader.getThreadName() != null) {
                tags.put(JvmEvents.THREAD_NAME, reader.getThreadName());
            }
            if (reader.getThreadState() != null) {
                tags.put(JvmEvents.THREAD_STATE, reader.getThreadState().toString());
            }
        }

        @Override
        public long threadId() {
            return reader.getThreadId();
        }

        @Override
        public String threadName() {
            return reader.getThreadName();
        }

        @Override
        public State threadState() {
            return reader.getThreadState();
        }

        @Override
        public StackFrameList stackTrace() {
            return reader.getStackTrace();
        }

        @Override
        public long timestamp() {
            return reader.getTimestamp();
        }

        @Override
        public CounterCollection counters() {
            return counters;
        }

        @Override
        public TagCollection tags() {
            return tags;
        }
    }
}
