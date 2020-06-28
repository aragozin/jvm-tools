package org.gridkit.jvmtool.stacktrace.codec.json;

import java.io.IOException;
import java.util.Iterator;
import java.util.NoSuchElementException;

import org.gridkit.jvmtool.event.CommonEvent;
import org.gridkit.jvmtool.event.Event;
import org.gridkit.jvmtool.event.EventMorpher;
import org.gridkit.jvmtool.event.EventReader;
import org.gridkit.jvmtool.event.MorphingEventReader;
import org.gridkit.jvmtool.spi.parsers.JsonEventSource;

public class JfrEventReader implements EventReader<CommonEvent> {

    private final JsonEventSource source;
    private final JsonEventAdapter adapter;
    private CommonEvent next;

    public JfrEventReader(JsonEventSource source, JsonEventAdapter adapter) {
        this.source = source;
        this.adapter = adapter;
    }

    @Override
    public boolean hasNext() {
        try {
            if (next == null) {
                next = adapter.parseNextEvent(source);
            }
            return next != null;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public CommonEvent next() {
        if (!hasNext()) {
            throw new NoSuchElementException();
        }
        CommonEvent e = next;
        next = null;
        return e;
    }

    @Override
    public void remove() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Iterator<CommonEvent> iterator() {
        return this;
    }

    @Override
    public <M extends Event> EventReader<M> morph(EventMorpher<CommonEvent, M> morpher) {
        return MorphingEventReader.morph(this, morpher);
    }

    @Override
    public CommonEvent peekNext() {
        return next;
    }

    @Override
    public void dispose() {
        // TODO
    }
}
